package com.earseo.core.service;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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

    @Value("${API_KEY}")
    private String ApiKeys;

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

    @Transactional
    public List<MiddleDataDto> getMiddleData(List<FilteredDataDto> filteredData, int start) {
        List<MiddleDataDto> middleData = new ArrayList<>();
        this.key = ApiKeys.split(",")[0];
        this.index = 0;
        System.out.println(filteredData.size());
        for (FilteredDataDto filteredDataDto : filteredData) {

            String contentId = filteredDataDto.contentId();
            String contentTypeId = filteredDataDto.contentTypeId();

            JsonNode common = fetchTourApi("https://apis.data.go.kr/B551011/KorService2/detailCommon2", contentId, null);
            DetailItemDto detail = fetchTourDetailApi("https://apis.data.go.kr/B551011/KorService2/detailIntro2", contentId, contentTypeId);
            JsonNode image = fetchTourApi("https://apis.data.go.kr/B551011/KorService2/detailImage2", contentId, null);

            ImageItemDto imageItemDto = null;
            CommonItemDto commonItemDto = null;

            if (common == null || image == null) continue;

            JsonNode commonItems = common
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            JsonNode imageItems = image
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            JsonNode commonItem = commonItems.get(0);
            JsonNode imageItem = imageItems.get(0);

            if (imageItem == null) imageItemDto = new ImageItemDto(null, null);
            else
                imageItemDto = new ImageItemDto(imageItem.get("originimgurl").asText(), imageItem.get("smallimageurl").asText());

            if (commonItem == null)
                commonItemDto = new CommonItemDto(null, null, null, null, null, null, null, null, null);
            else commonItemDto = parseCommonItem(commonItem);

            middleData.add(new MiddleDataDto(contentId, contentTypeId, filteredDataDto.cat1(), filteredDataDto.cat2(), filteredDataDto.cat3(),
                    filteredDataDto.outl(), commonItemDto.title(), commonItemDto.addr1(), commonItemDto.addr2(), commonItemDto.mapX(), commonItemDto.mapY(),
                    commonItemDto.modifiedTime(), commonItemDto.tel(), commonItemDto.mLevel(), commonItemDto.overview(),
                    imageItemDto.imgrul(), imageItemDto.smallimgurl(), detail.usetime(), detail.restdate(), detail.parking(), detail.usefee()
            ));

        }
        List<MiddleData> middleDataList = middleData.stream().map(MiddleData::new).toList();
        middleRepository.saveAll(middleDataList);
        return middleData;

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

        JsonNode jsonNode = null;

        try {
            jsonNode = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

        } catch (Exception e) {
            if (this.index + 1 != ApiKeys.split(",").length) {
                this.key = ApiKeys.split(",")[index + 1];
                this.index++;
                return fetchTourApi(url, contentId, contentTypeId);
            }
        }

        return jsonNode;
    }

    public DetailItemDto fetchTourDetailApi(String url, String contentId, String contentTypeId) {
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
        JsonNode jsonNode = null;
        try {
            jsonNode = client.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

        } catch (Exception e) {
            if (this.index + 1 != ApiKeys.split(",").length) {
                this.key = ApiKeys.split(",")[index + 1];
                this.index++;
                return fetchTourDetailApi(url, contentId, contentTypeId);
            }
        }
        DetailItemDto detailItemDto;

        if (jsonNode == null) return detailItemDto = new DetailItemDto(null, null, null, null);

        JsonNode detailItem = jsonNode
                .path("response")
                .path("body")
                .path("items")
                .path("item")
                .get(0);

        if (detailItem == null) return detailItemDto = new DetailItemDto(null, null, null, null);
        switch (contentTypeId) {
            case "12":
                detailItemDto = new DetailItemDto(null,
                        detailItem.get("parking").asText(), detailItem.get("restdate").asText(), detailItem.get("usetime").asText());
                break;
            case "14":
                detailItemDto = new DetailItemDto(detailItem.get("usefee").asText(),
                        detailItem.get("parkingculture").asText(), detailItem.get("restdateculture").asText(), detailItem.get("usetimeculture").asText());
                break;
            case "15":
                detailItemDto = new DetailItemDto(detailItem.get("usetimefestival").asText(),
                        null, null, null);
                break;
            case "28":
                detailItemDto = new DetailItemDto(detailItem.get("usefeeleports").asText(),
                        detailItem.get("parkingleports").asText(), detailItem.get("restdateleports").asText(), detailItem.get("usetimeleports").asText());
                break;
            case "38":
                detailItemDto = new DetailItemDto(null,
                        detailItem.get("parkingshopping").asText(), null, null);
                break;

            default:
                detailItemDto = new DetailItemDto(null, null, null, null);
        }

        return detailItemDto;
    }

    private CommonItemDto parseCommonItem(JsonNode commonItem) {
        return new CommonItemDto(
                commonItem.path("title").asText(),
                commonItem.path("addr1").asText(),
                commonItem.path("addr2").asText(),
                commonItem.path("mapx").asText(),
                commonItem.path("mapy").asText(),
                commonItem.path("modifiedtime").asText(),
                commonItem.path("tel").asText(),
                commonItem.path("mlevel").asText(),
                commonItem.path("overview").asText()
        );
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

        masterRepository.saveAll(masters);
        objectMapper
                .getFactory()
                .setStreamWriteConstraints(
                        StreamWriteConstraints.builder()
                                .maxNestingDepth(3000)
                                .build()
                );
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new java.io.File("master_data.json"), masterItemDtos); // S3 저장으로 리팩토링 예정
    }
}
