package com.earseo.core.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.earseo.core.dto.etl.*;
import com.earseo.core.dto.tourApi.AreaResponse;
import com.earseo.core.dto.tourApi.CommonResponse;
import com.earseo.core.dto.tourApi.DetailResponse;
import com.earseo.core.entity.*;
import com.earseo.core.repository.*;
import com.earseo.core.service.master.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CategoryRepository categoryRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final AmazonS3 amazonS3;
    private final SpotCategoryRepository spotCategoryRepository;
    private final KoMasterRepository koMasterRepository;
    private final EnMasterRepository enMasterRepository;

    @Value("${api.key}")
    private String ApiKeys;
    @Value(("${cloud.aws.s3.bucket}"))
    private String bucketName;

    private final Integer NUM_OF_ROWS = 100;
    private final Integer BATCH_SIZE = 15;

    private List<String> keyList;
    private int keyIndex = 0;

    @PostConstruct
    public void init() {
        this.keyList = Arrays.asList(ApiKeys.split(","));
    }

    private String getValidKey() {
        return keyList.get(keyIndex);
    }

    private void rotateKey() {
        keyIndex = (keyIndex + 1) % keyList.size();
    }

    private <T> T executeWithRetry(Supplier<T> apiCall) {
        int retryCount = 0;
        while (retryCount < keyList.size() * 2) {
            try {
                return apiCall.get();
            } catch (Exception e) {
                if (e instanceof org.springframework.web.client.HttpClientErrorException.TooManyRequests ||
                        e instanceof org.springframework.web.client.UnknownContentTypeException ||
                        e instanceof org.springframework.web.client.HttpServerErrorException ||
                        e.getMessage().contains("429") ||
                        e.getMessage().contains("502") ||
                        e.getMessage().contains("HttpMessageNotReadableException")) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    rotateKey();
                    retryCount++;
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("TOUR API 호출 에러");
    }

    public List<AreaItemDto> getTourApiArea(String tourApiPrefix) {
        return fetchTourApiArea(tourApiPrefix);
    }

    public void createMasterTable(List<AreaItemDto> areaItemList, String commonPath, String detailPath, String lang) throws IOException {
        int batchSize = BATCH_SIZE;
        List<MasterItemDto> masterItemList = new ArrayList<>();

        for (int i = 0; i < areaItemList.size(); i += batchSize) {
            List<AreaItemDto> batch =
                    areaItemList.subList(i, Math.min(i + batchSize, areaItemList.size()));

            masterItemList.addAll(processBatch(batch, commonPath, detailPath, lang));
        }

        loadData(masterItemList, lang);

    }

    @Transactional
    public void loadData(List<MasterItemDto> list, String lang) throws IOException {

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("ko".equals(lang)) {
            List<KoMaster> entities =
                    list.stream().map(MasterItemDto::toKo).toList();

            koMasterRepository.saveAll(entities);

            writeAndUploadJson(
                    entities,
                    "master_data_ko.json",
                    "master/master_data_ko_" + date + ".json"
            );

        } else if ("en".equals(lang)) {
            List<EnMaster> entities =
                    list.stream().map(MasterItemDto::toEn).toList();

            enMasterRepository.saveAll(entities);

            writeAndUploadJson(
                    entities,
                    "master_data_en.json",
                    "master/master_data_en_" + date + ".json"
            );
        }
    }

    private void writeAndUploadJson(
            Object data,
            String fileName,
            String s3Key
    ) throws IOException {

        File jsonFile = new File(fileName);

        objectMapper.writeValue(jsonFile, data);

        amazonS3.putObject(
                new PutObjectRequest(
                        bucketName,
                        s3Key,
                        jsonFile
                )
        );
    }

    public List<MasterItemDto> processBatch(List<AreaItemDto> batch, String commonPath, String detailPath, String lang) {
        List<MasterItemDto> masterItemDtoList = new ArrayList<>();

        for (AreaItemDto areaItemDto : batch) {

            DetailItemDto detailItemDto = fetchTourApiDetail(areaItemDto, detailPath);
            if (detailItemDto == null){
                log.info("디테일 비었음 : {} , {}", areaItemDto.contentTypeId(), areaItemDto.contentId());
                continue;}

            String overview = fetchTourApiCommon(areaItemDto, commonPath);
            if (overview == null){
                log.info("common 비었음 : {} , {}", areaItemDto.contentTypeId(), areaItemDto.contentId());
                continue;
            }

            SubCategoryGroup subCat = SubCategoryGroup.fromCode(areaItemDto.catCode());
            CategoryGroup cat = subCat.getCategoryGroup();

            String cat1, cat2, cat1Code, cat2Code;
            if (lang.equals("ko")) {
                cat1 = cat.getKoName();
                cat2 = subCat.getKoName();
                cat1Code = cat.getCode();
                cat2Code = subCat.getCode();
            } else {
                cat1 = cat.getEnName();
                cat2 = cat.getEnName();
                cat1Code = cat.getCode();
                cat2Code = cat.getCode();
            }

            MasterItemDto masterItemDto = MasterItemDto.builder()
                    .contentId(areaItemDto.contentId())
                    .cat1(cat1)
                    .cat2(cat2)
                    .cat1Code(cat1Code)
                    .cat2Code(cat2Code)
                    .contentTypeId(areaItemDto.contentTypeId())
                    .addr1(areaItemDto.address())
                    .addr2(areaItemDto.detailAddress())
                    .addr3(parseSeoulAddr(areaItemDto.address()))
                    .mapX(areaItemDto.mapX())
                    .mapY(areaItemDto.mapY())
                    .mLevel(areaItemDto.mlevel())
                    .modifiedtime(areaItemDto.modifiedtime())
                    .originImgUrl(areaItemDto.imageUrl())
                    .title(areaItemDto.title())
                    .smallImgUrl(areaItemDto.thumbnailUrl())
                    .overview(overview)
                    .parking(detailItemDto.parking())
                    .usefee(detailItemDto.usefee())
                    .restdate(detailItemDto.restdate())
                    .tel(areaItemDto.tel())
                    .usetime(detailItemDto.usetime())
                    .build();
            masterItemDtoList.add(masterItemDto);
        }

        return masterItemDtoList;
    }

    public List<AreaItemDto> fetchTourApiArea(String path) {
        List<AreaResponse.Item> result = new ArrayList<>();

        int numOfRow = 100;
        int pageNo = 1;
        int now = 0;

        do {
            final int currentPage = pageNo;

            AreaResponse response = executeWithRetry(() ->
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(path)
                                    .queryParam("serviceKey", getValidKey())
                                    .queryParam("MobileApp", "AppTest")
                                    .queryParam("MobileOS", "ETC")
                                    .queryParam("_type", "json")
                                    .queryParam("numOfRows", numOfRow)
                                    .queryParam("pageNo", currentPage)
                                    .queryParam("areaCode", "1")
                                    .build()
                            )
                            .retrieve()
                            .body(AreaResponse.class)
            );

            AreaResponse.Body body = response.response().body();

            List<AreaResponse.Item> items =
                    body.items() != null ? body.items().item() : List.of();

            result.addAll(items);
            now = body.numOfRows();
            pageNo++;

        } while (now == numOfRow);
        log.info("Tour API AREA : DONE");
        return result.stream()
                .filter(ExcludeRule::isInclude)
                .map(AreaItemDto::from).toList();
    }

    public DetailItemDto fetchTourApiDetail(AreaItemDto areaItemDto, String prefix) {
        DetailType detailType = DetailType.from(areaItemDto.contentTypeId());

        DetailResponse response = executeWithRetry(() ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(prefix)
                                .queryParam("contentId", areaItemDto.contentId())
                                .queryParam("contentTypeId", areaItemDto.contentTypeId())
                                .queryParam("serviceKey", getValidKey())
                                .queryParam("MobileApp", "AppTest")
                                .queryParam("MobileOS", "ETC")
                                .queryParam("_type", "json")
                                .build()
                        )
                        .retrieve()
                        .body(detailType.getResponseType())
        );
        return (response == null) ? null : response.getDetailItemDto();
    }

    public String fetchTourApiCommon(AreaItemDto areaItemDto, String prefix) {
        CommonResponse response = executeWithRetry(() ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(prefix)
                                .queryParam("contentId", areaItemDto.contentId())
                                .queryParam("serviceKey", getValidKey())
                                .queryParam("MobileApp", "AppTest")
                                .queryParam("MobileOS", "ETC")
                                .queryParam("_type", "json")
                                .build()
                        )
                        .retrieve()
                        .body(CommonResponse.class)
        );

        if (response == null || response.response() == null || response.response().body().items() == null) {
            log.info("[비었음] 컨텐츠 아이디 : {} ", areaItemDto.contentId());
            return null;
        }
        return response.response().body().items().item().get(0).overview();
    }

    @Transactional
    public void createCategory() {
        List<Category> categories = categoryRepository.findAll();

        List<SpotCategory> spotCategories = categories.stream()
                .filter(category -> category.getCode().length() == 5)
                .map(category -> SubCategoryGroup.from(category)) // Optional<SubCategoryGroup>
                .filter(Optional::isPresent) // 없는 건 걸러내기
                .map(Optional::get)
                .map(subCategoryGroup -> SpotCategory.builder()
                        .originCode(subCategoryGroup.getCode())
                        .code(subCategoryGroup.getCode())
                        .EngName(subCategoryGroup.getEnName())
                        .KoName(subCategoryGroup.getKoName())
                        .parentCode(subCategoryGroup.getCategoryGroup().getCode())
                        .parentEnName(subCategoryGroup.getCategoryGroup().getEnName())
                        .parentKoName(subCategoryGroup.getCategoryGroup().getKoName())
                        .build())
                .toList();
        spotCategoryRepository.saveAll(spotCategories);
    }

    private String parseSeoulAddr(String addr) {
        if (addr == null) return null;

        Pattern pattern = Pattern.compile("^서울특별시\\s+(\\S+)");
        Matcher matcher = pattern.matcher(addr);

        if (matcher.find()) {
            return "서울시 " + matcher.group(1);
        }

        return null;
    }
}