package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.etl.FilteredDataDto;
import com.earseo.core.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/admin/master")
    public ResponseEntity<BaseResponse<String>> rawDataProcess(){
        List<FilteredDataDto> filteredData = masterDataService.getRawInfo();
        return ResponseEntity.ok(BaseResponse.ok(null));
    }
}
