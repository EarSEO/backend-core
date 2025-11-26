package com.earseo.core.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.etl.*;
import com.earseo.core.entity.Category;
import com.earseo.core.entity.Master;
import com.earseo.core.entity.MiddleData;
import com.earseo.core.repository.CategoryRepository;
import com.earseo.core.repository.MasterRepository;
import com.earseo.core.repository.MiddleRepository;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.Json;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final MiddleRepository middleRepository;
    private final MasterRepository masterRepository;
    private final GeometryFactory  geometryFactory = new GeometryFactory();
    private final AmazonS3 amazonS3;

    @Value("${api.key}")
    private String ApiKeys;
    @Value(("${cloud.aws.s3.bucket}"))
    private String bucketName;

    private String key;
    private int index;

    public List<FilteredDataDto> getRawInfo() {
        try {
            InputStream rawJson = new ClassPathResource("TourAPI_seoul.json").getInputStream();
            List<FilteredDataDto> rawJsonDtos = objectMapper.readValue(
                    rawJson,
                    new TypeReference<List<FilteredDataDto>>() {
                    }
            );

            List<FilteredDataDto> filtered = rawJsonDtos.stream()
                    .filter(dto -> !dto.contentTypeId().equals("25"))
                    .filter(dto -> !dto.contentTypeId().equals("32"))
                    .filter(dto -> !dto.contentTypeId().equals("39"))
                    .filter(dto -> !dto.cat3().equals("A04011000"))
                    .toList();

            return filtered;

        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public void initData() {
        List<CategoryItemDto> cat1s = fetchCategoryApi(null, null);
        List<CategoryItemDto> allCategories = new ArrayList<>();
        allCategories.addAll(cat1s);

        for (CategoryItemDto cat1 : cat1s) {
            List<CategoryItemDto> cat2s = fetchCategoryApi(cat1.code(), null);
            allCategories.addAll(cat2s);
            for (CategoryItemDto cat2 : cat2s) {
                List<CategoryItemDto> cat3s = fetchCategoryApi(cat1.code(), cat2.code());
                allCategories.addAll(cat3s);
            }
        }

        List<Category> categories = allCategories.stream()
                .map(c -> Category.builder()
                        .code(c.code())
                        .name(c.name())
                        .build())
                .toList();

        categoryRepository.saveAll(categories);
    }


    public List<CategoryItemDto> fetchCategoryApi(String cat1, String cat2) {
        String key = ApiKeys.split(",")[0];

        RestClient client = RestClient.create();

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://apis.data.go.kr/B551011/KorService2/categoryCode2")
                .queryParam("serviceKey", key)
                .queryParam("MobileApp", "AppTest")
                .queryParam("MobileOS", "ETC")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("_type", "json")
                .queryParamIfPresent("cat1", Optional.ofNullable(cat1))
                .queryParamIfPresent("cat2", Optional.ofNullable(cat2))
                .build(true)
                .toUri();
        try {
            JsonNode jsonNode = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode items = jsonNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            List<CategoryItemDto> list = new ArrayList<>();

            for (JsonNode item : items) {
                list.add(new CategoryItemDto(item.get("code").asText(), item.get("name").asText()));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transactional
    public void createMasterTable() throws IOException {
        List<MiddleData> middleDataList = middleRepository.findAll();
        List<MasterItemDto> masterItemDtos = new ArrayList<>();
        List<Master> masters = new ArrayList<>();

        for (MiddleData middle : middleDataList) {
            String addr3 = null;
            if (middle.getAddr1() != null && middle.getAddr1().startsWith("서울특별시")) {
                addr3 = "서울시 " + middle.getAddr1().split(" ")[1];
            }

            Point geom = null;
            try {
                if (middle.getMapX() != null && middle.getMapY() != null) {
                    double x = Double.parseDouble(middle.getMapX());
                    double y = Double.parseDouble(middle.getMapY());
                    geom = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(x, y));
                }
            } catch (NumberFormatException e) {
                geom = null;
            }

            Master master = Master.builder()
                    .contentId(middle.getContentId())
                    .contentTypeId(middle.getContentTypeId())
                    .cat1(middle.getCat1())
                    .cat2(middle.getCat2())
                    .cat3(middle.getCat3())
                    .ocat1(middle.getCat1())
                    .ocat2(middle.getCat2())
                    .ocat3(middle.getCat3())
                    .outl(middle.getOutl())
                    .title(middle.getTitle())
                    .addr1(middle.getAddr1())
                    .addr2(middle.getAddr2())
                    .addr3(addr3)
                    .mapX(middle.getMapX() != null ? Double.valueOf(middle.getMapX()) : null)
                    .mapY(middle.getMapY() != null ? Double.valueOf(middle.getMapY()) : null)
                    .modifiedtime(middle.getModifiedtime())
                    .tel(middle.getTel())
                    .mLevel(
                            (middle.getMLevel() != null && !middle.getMLevel().isBlank()) ? Integer.parseInt(middle.getMLevel().trim()) : null
                    )
                    .overview(middle.getOverview())
                    .originImgUrl(middle.getOriginImgUrl())
                    .smallImgUrl(middle.getSmallImgUrl())
                    .usetime(middle.getUsetime())
                    .restdate(middle.getRestdate())
                    .parking(middle.getParking())
                    .usefee(middle.getUsefee())
                    .geom(geom)
                    .build();

            masters.add(master);

            MasterItemDto dto = new MasterItemDto(
                    master.getContentId(), master.getContentTypeId(), master.getCat1(), master.getCat2(), master.getCat3(),
                    master.getOcat1(), master.getOcat2(), master.getOcat3(), master.getOutl(), master.getTitle(),
                    master.getAddr1(), master.getAddr2(), master.getAddr3(), master.getMapX(), master.getMapY(),
                    master.getModifiedtime(), master.getTel(), master.getMLevel(), master.getOverview(), master.getOriginImgUrl(),
                    master.getSmallImgUrl(), master.getUsetime(), master.getRestdate(), master.getParking(), master.getUsefee()
            );

            masterItemDtos.add(dto);
        }

        File jsonFile = new File("master_data.json");

        masterRepository.saveAll(masters);
        objectMapper
                .getFactory()
                .setStreamWriteConstraints(
                        StreamWriteConstraints.builder()
                                .maxNestingDepth(3000)
                                .build()
                );

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(jsonFile, masterItemDtos); // S3 저장으로 리팩토링 예정

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String s3Key = "master/master_data_" + date + ".json";

        amazonS3.putObject(
                new PutObjectRequest(
                        bucketName,
                        s3Key,
                        jsonFile
                )
        );
    }

    @Transactional
    public List<MiddleDataDto> getMiddleData(List<FilteredDataDto> filteredData) {

        this.key = ApiKeys.split(",")[0];
        this.index = 0;

        List<MiddleDataDto> middleDataList = new ArrayList<>();

        for (FilteredDataDto filtered : filteredData) {

            String contentId = filtered.contentId();
            String contentTypeId = filtered.contentTypeId();

            JsonNode commonNode = fetchTourApi(
                    "https://apis.data.go.kr/B551011/KorService2/detailCommon2",
                    contentId,
                    null
            );

            JsonNode detailNode = fetchTourApi(
                    "https://apis.data.go.kr/B551011/KorService2/detailIntro2",
                    contentId,
                    contentTypeId
            );

            JsonNode imageNode = fetchTourApi(
                    "https://apis.data.go.kr/B551011/KorService2/detailImage2",
                    contentId,
                    null
            );

            if (commonNode == null || detailNode == null || imageNode == null) continue;

            JsonNode commonItem = getItem(commonNode);
            JsonNode detailItem = getItem(detailNode);
            JsonNode imageItem = getItem(imageNode);

            CommonItemDto commonDto = parseCommon(commonItem);
            DetailItemDto detailDto = parseDetail(detailItem, contentTypeId);
            ImageItemDto imageDto = parseImage(imageItem);

            MiddleDataDto dto = new MiddleDataDto(
                    contentId,
                    contentTypeId,
                    filtered.cat1(),
                    filtered.cat2(),
                    filtered.cat3(),
                    filtered.outl(),
                    commonDto.title(),
                    commonDto.addr1(),
                    commonDto.addr2(),
                    commonDto.mapX(),
                    commonDto.mapY(),
                    commonDto.modifiedTime(),
                    commonDto.tel(),
                    commonDto.mLevel(),
                    commonDto.overview(),
                    imageDto.imgrul(),
                    imageDto.smallimgurl(),
                    detailDto.usetime(),
                    detailDto.restdate(),
                    detailDto.parking(),
                    detailDto.usefee()
            );

            middleDataList.add(dto);
        }

        middleRepository.saveAll(
                middleDataList.stream()
                        .map(MiddleData::new)
                        .toList()
        );

        return middleDataList;
    }

    public JsonNode fetchTourApi(String url, String contentId, String contentTypeId) {

        RestClient client = RestClient.create();

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("serviceKey", this.key)
                .queryParam("MobileApp", "AppTest")
                .queryParam("MobileOS", "ETC")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("_type", "json")
                .queryParamIfPresent("contentId", Optional.ofNullable(contentId))
                .queryParamIfPresent("contentTypeId", Optional.ofNullable(contentTypeId))
                .build(true)
                .toUri();

        try {
            return client.get().uri(uri).retrieve().body(JsonNode.class);

        } catch (Exception e) {
            // key 변경 후 retry
            if (this.index + 1 < ApiKeys.split(",").length) {
                this.index++;
                this.key = ApiKeys.split(",")[this.index];
                return fetchTourApi(url, contentId, contentTypeId);
            }
            return null;
        }
    }

    private JsonNode getItem(JsonNode root) {
        return root.path("response").path("body")
                .path("items").path("item").get(0);
    }

    private CommonItemDto parseCommon(JsonNode item) {
        if (item == null) {
            return new CommonItemDto(null, null, null, null, null, null, null, null, null);
        }

        return new CommonItemDto(
                item.path("title").asText(),
                item.path("addr1").asText(),
                item.path("addr2").asText(),
                item.path("mapx").asText(),
                item.path("mapy").asText(),
                item.path("modifiedtime").asText(),
                item.path("tel").asText(),
                item.path("mlevel").asText(),
                item.path("overview").asText()
        );
    }

    private ImageItemDto parseImage(JsonNode item) {
        if (item == null) return new ImageItemDto(null, null);

        return new ImageItemDto(
                item.path("originimgurl").asText(null),
                item.path("smallimageurl").asText(null)
        );
    }

    private DetailItemDto parseDetail(JsonNode item, String typeId) {
        if (item == null) return new DetailItemDto(null, null, null, null);

        return switch (typeId) {
            case "12" -> new DetailItemDto(
                    null,
                    item.path("parking").asText(),
                    item.path("restdate").asText(),
                    item.path("usetime").asText()
            );
            case "14" -> new DetailItemDto(
                    item.path("usefee").asText(),
                    item.path("parkingculture").asText(),
                    item.path("restdateculture").asText(),
                    item.path("usetimeculture").asText()
            );
            case "15" -> new DetailItemDto(
                    item.path("usetimefestival").asText(),
                    null, null, null
            );
            case "28" -> new DetailItemDto(
                    item.path("usefeeleports").asText(),
                    item.path("parkingleports").asText(),
                    item.path("restdateleports").asText(),
                    item.path("usetimeleports").asText()
            );
            case "38" -> new DetailItemDto(
                    null,
                    item.path("parkingshopping").asText(),
                    null,
                    null
            );
            default -> new DetailItemDto(null, null, null, null);
        };
    }
}
