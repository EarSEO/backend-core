package com.earseo.core.service;

import com.earseo.core.dto.etl.FilteredDataDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${API_KEY}")
    private String ApiKeys;

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

}
