package com.earseo.core.service;

import com.earseo.core.entity.OdiiData;
import com.earseo.core.repository.OdiiDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocentService {

    private final OdiiDataRepository odiiDataRepository;

    @Value("${api.key}")
    private String ApiKeys;
    @Value(("${cloud.aws.s3.bucket}"))
    private String bucketName;

    private String key;
    private int index;

    @Transactional
    public void initDocent(){

        int pageNo = 1;
        int numOfRows = 100;

        while(true){
            JsonNode jsonNode = fetchOdiiApi(pageNo, numOfRows);
            JsonNode body = jsonNode.path("response").path("body");

            if(body.path("numOfRows").asInt() == 0){
                break;
            }

            List<OdiiData> odiiDataList = new ArrayList<>();

            if(body.path("items").path("item").isArray()){
                System.out.println("hello");
                for(JsonNode item : body.path("items").path("item")){
                    odiiDataList.add(OdiiData.builder()
                                    .title(item.path("title").asText())
                                    .script(item.path("script").asText())
                            .build());
                }
            }

            odiiDataRepository.saveAll(odiiDataList);
            pageNo++;
        }
    }

    public JsonNode fetchOdiiApi(int pageNo, int numOfRows) {
        this.key = ApiKeys.split(",")[0];
        this.index = 0;
        RestClient client = RestClient.create();

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://apis.data.go.kr/B551011/Odii/storyBasedList")
                .queryParam("serviceKey", this.key)
                .queryParam("MobileApp", "AppTest")
                .queryParam("MobileOS", "ETC")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("_type", "json")
                .queryParam("langCode", "ko")
                .build(true)
                .toUri();
        try {
            return client.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (this.index + 1 < ApiKeys.split(",").length) {
                this.index++;
                this.key = ApiKeys.split(",")[this.index];
                return fetchOdiiApi(pageNo, numOfRows);
            }
            return null;
        }
    }

}
