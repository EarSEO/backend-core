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
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    public List<AreaItemDto> getTourApiArea(String tourApiPrefix) {
        return fetchTourApiArea(tourApiPrefix);
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
            if (detailItemDto == null) continue;

            String overview = fetchTourApiCommon(areaItemDto, commonPath);
            if (overview == null) continue;

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
                                .queryParam("contentTypeId", detailType.getId())
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

    public void createMaster() {

        //ist<NewMiddleItemDto> middleItemDtos = fetchTourApiCommon(areaItemDtos);
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

//
//    public List<FilteredDataDto> getRawInfo() {
//        try {
//            InputStream rawJson = new ClassPathResource("TourAPI_seoul.json").getInputStream();
//            List<FilteredDataDto> rawJsonDtos = objectMapper.readValue(
//                    rawJson,
//                    new TypeReference<List<FilteredDataDto>>() {
//                    }
//            );
//
//            List<FilteredDataDto> filtered = rawJsonDtos.stream()
//                    .filter(dto -> !dto.contentTypeId().equals("25"))
//                    .filter(dto -> !dto.contentTypeId().equals("32"))
//                    .filter(dto -> !dto.contentTypeId().equals("39"))
//                    .filter(dto -> !dto.cat3().equals("A04011000"))
//                    .toList();
//
//            return filtered;
//
//        } catch (Exception e) {
//            return List.of();
//        }
//    }
//}
//    @Transactional
//    public void createMasterTable() throws IOException {
//        List<MiddleData> middleDataList = middleRepository.findAll();
//        List<MasterItemDto> masterItemDtos = new ArrayList<>();
//        List<Master> masters = new ArrayList<>();
//
//        for (MiddleData middle : middleDataList) {
//            String addr3 = null;
//            if (middle.getAddr1() != null && middle.getAddr1().startsWith("서울특별시")) {
//                addr3 = "서울시 " + middle.getAddr1().split(" ")[1];
//            }
//
//            Point geom = null;
//            try {
//                if (middle.getMapX() != null && middle.getMapY() != null) {
//                    double x = Double.parseDouble(middle.getMapX());
//                    double y = Double.parseDouble(middle.getMapY());
//                    geom = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(x, y));
//                }
//            } catch (NumberFormatException e) {
//                geom = null;
//            }
//
//            Master master = Master.builder()
//                    .contentId(middle.getContentId())
//                    .contentTypeId(middle.getContentTypeId())
//                    .cat1(middle.getCat1())
//                    .cat2(middle.getCat2())
//                    .cat3(middle.getCat3())
//                    .ocat1(middle.getCat1())
//                    .ocat2(middle.getCat2())
//                    .ocat3(middle.getCat3())
//                    .outl(middle.getOutl())
//                    .title(middle.getTitle())
//                    .addr1(middle.getAddr1())
//                    .addr2(middle.getAddr2())
//                    .addr3(addr3)
//                    .mapX(middle.getMapX() != null ? Double.valueOf(middle.getMapX()) : null)
//                    .mapY(middle.getMapY() != null ? Double.valueOf(middle.getMapY()) : null)
//                    .modifiedtime(middle.getModifiedtime())
//                    .tel(middle.getTel())
//                    .mLevel(
//                            (middle.getMLevel() != null && !middle.getMLevel().isBlank()) ? Integer.parseInt(middle.getMLevel().trim()) : null
//                    )
//                    .overview(middle.getOverview())
//                    .originImgUrl(middle.getOriginImgUrl())
//                    .smallImgUrl(middle.getSmallImgUrl())
//                    .usetime(middle.getUsetime())
//                    .restdate(middle.getRestdate())
//                    .parking(middle.getParking())
//                    .usefee(middle.getUsefee())
//                    .geom(geom)
//                    .build();
//
//            masters.add(master);
//
//            MasterItemDto dto = new MasterItemDto(
//                    master.getContentId(), master.getContentTypeId(), master.getCat1(), master.getCat2(), master.getCat3(),
//                    master.getOcat1(), master.getOcat2(), master.getOcat3(), master.getOutl(), master.getTitle(),
//                    master.getAddr1(), master.getAddr2(), master.getAddr3(), master.getMapX(), master.getMapY(),
//                    master.getModifiedtime(), master.getTel(), master.getMLevel(), master.getOverview(), master.getOriginImgUrl(),
//                    master.getSmallImgUrl(), master.getUsetime(), master.getRestdate(), master.getParking(), master.getUsefee()
//            );
//
//            masterItemDtos.add(dto);
//        }
//
//        File jsonFile = new File("master_data.json");
//
//        masterRepository.saveAll(masters);
//        objectMapper
//                .getFactory()
//                .setStreamWriteConstraints(
//                        StreamWriteConstraints.builder()
//                                .maxNestingDepth(3000)
//                                .build()
//                );
//
//        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
//        objectMapper.writeValue(jsonFile, masterItemDtos); // S3 저장으로 리팩토링 예정
//
//        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        String s3Key = "master/master_data_" + date + ".json";
//
//        amazonS3.putObject(
//                new PutObjectRequest(
//                        bucketName,
//                        s3Key,
//                        jsonFile
//                )
//        );
//    }
//
//    @Transactional
//    public List<MiddleDataDto> getMiddleData(List<FilteredDataDto> filteredData) {
//
////        this.key = ApiKeys.split(",")[0];
////        this.index = 0;
//
//        List<MiddleDataDto> middleDataList = new ArrayList<>();
//
//        for (FilteredDataDto filtered : filteredData) {
//
//            String contentId = filtered.contentId();
//            String contentTypeId = filtered.contentTypeId();
//
//            JsonNode commonNode = fetchTourApi(
//                    "https://apis.data.go.kr/B551011/KorService2/detailCommon2",
//                    contentId,
//                    null
//            );
//
//            JsonNode detailNode = fetchTourApi(
//                    "https://apis.data.go.kr/B551011/KorService2/detailIntro2",
//                    contentId,
//                    contentTypeId
//            );
//
//            JsonNode imageNode = fetchTourApi(
//                    "https://apis.data.go.kr/B551011/KorService2/detailImage2",
//                    contentId,
//                    null
//            );
//
//            if (commonNode == null || detailNode == null || imageNode == null) continue;
//
//            JsonNode commonItem = getItem(commonNode);
//            JsonNode detailItem = getItem(detailNode);
//            JsonNode imageItem = getItem(imageNode);
//
//            CommonItemDto commonDto = parseCommon(commonItem);
//            DetailItemDto detailDto = parseDetail(detailItem, contentTypeId);
//            ImageItemDto imageDto = parseImage(imageItem);
//
//            MiddleDataDto dto = new MiddleDataDto(
//                    contentId,
//                    contentTypeId,
//                    filtered.cat1(),
//                    filtered.cat2(),
//                    filtered.cat3(),
//                    filtered.outl(),
//                    commonDto.title(),
//                    commonDto.addr1(),
//                    commonDto.addr2(),
//                    commonDto.mapX(),
//                    commonDto.mapY(),
//                    commonDto.modifiedTime(),
//                    commonDto.tel(),
//                    commonDto.mLevel(),
//                    commonDto.overview(),
//                    imageDto.imgrul(),
//                    imageDto.smallimgurl(),
//                    detailDto.usetime(),
//                    detailDto.restdate(),
//                    detailDto.parking(),
//                    detailDto.usefee()
//            );
//
//            middleDataList.add(dto);
//        }
//
//        middleRepository.saveAll(
//                middleDataList.stream()
//                        .map(MiddleData::new)
//                        .toList()
//        );
//
//        return middleDataList;
//    }
//
////    public JsonNode fetchTourApi(String url, String contentId, String contentTypeId) {
////
////        RestClient client = RestClient.create();
////
////        URI uri = UriComponentsBuilder
////                .fromHttpUrl(url)
////                .queryParam("serviceKey", this.key)
////                .queryParam("MobileApp", "AppTest")
////                .queryParam("MobileOS", "ETC")
////                .queryParam("pageNo", 1)
////                .queryParam("numOfRows", 10)
////                .queryParam("_type", "json")
////                .queryParamIfPresent("contentId", Optional.ofNullable(contentId))
////                .queryParamIfPresent("contentTypeId", Optional.ofNullable(contentTypeId))
////                .build(true)
////                .toUri();
////
////        try {
////            return client.get().uri(uri).retrieve().body(JsonNode.class);
////
////        } catch (Exception e) {
//////            // key 변경 후 retry
//////            if (this.index + 1 < ApiKeys.split(",").length) {
//////                this.index++;
//////                this.key = ApiKeys.split(",")[this.index];
//////                return fetchTourApi(url, contentId, contentTypeId);
//////            }
////            return null;
////        }
////    }
//
//    private JsonNode getItem(JsonNode root) {
//        return root.path("response").path("body")
//                .path("items").path("item").get(0);
//    }
//
//    private CommonItemDto parseCommon(JsonNode item) {
//        if (item == null) {
//            return new CommonItemDto(null, null, null, null, null, null, null, null, null);
//        }
//
//        return new CommonItemDto(
//                item.path("title").asText(),
//                item.path("addr1").asText(),
//                item.path("addr2").asText(),
//                item.path("mapx").asText(),
//                item.path("mapy").asText(),
//                item.path("modifiedtime").asText(),
//                item.path("tel").asText(),
//                item.path("mlevel").asText(),
//                item.path("overview").asText()
//        );
//    }
//
//    private ImageItemDto parseImage(JsonNode item) {
//        if (item == null) return new ImageItemDto(null, null);
//
//        return new ImageItemDto(
//                item.path("originimgurl").asText(null),
//                item.path("smallimageurl").asText(null)
//        );
//    }
//
//    private DetailItemDto parseDetail(JsonNode item, String typeId) {
//        if (item == null) return new DetailItemDto(null, null, null, null);
//
//        return switch (typeId) {
//            case "12" -> new DetailItemDto(
//                    null,
//                    item.path("parking").asText(),
//                    item.path("restdate").asText(),
//                    item.path("usetime").asText()
//            );
//            case "14" -> new DetailItemDto(
//                    item.path("usefee").asText(),
//                    item.path("parkingculture").asText(),
//                    item.path("restdateculture").asText(),
//                    item.path("usetimeculture").asText()
//            );
//            case "15" -> new DetailItemDto(
//                    item.path("usetimefestival").asText(),
//                    null, null, null
//            );
//            case "28" -> new DetailItemDto(
//                    item.path("usefeeleports").asText(),
//                    item.path("parkingleports").asText(),
//                    item.path("restdateleports").asText(),
//                    item.path("usetimeleports").asText()
//            );
//            case "38" -> new DetailItemDto(
//                    null,
//                    item.path("parkingshopping").asText(),
//                    null,
//                    null
//            );
//            default -> new DetailItemDto(null, null, null, null);
//        };
//    }
//}
