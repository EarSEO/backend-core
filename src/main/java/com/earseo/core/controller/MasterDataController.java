package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.etl.FilteredDataDto;
import com.earseo.core.dto.etl.MiddleDataDto;
import com.earseo.core.service.MasterDataService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/admin/core/master/{start}")
    public ResponseEntity<BaseResponse<String>> rawDataProcess(@PathVariable int start){
        List<FilteredDataDto> filteredData = masterDataService.getRawInfo();
        List<MiddleDataDto> middleData = masterDataService.getMiddleData(filteredData,start);
        return ResponseEntity.ok(BaseResponse.ok(null));
    }

    @GetMapping("/admin/core/init")
    public ResponseEntity<BaseResponse<String>> init(){
        masterDataService.initData();
        return ResponseEntity.ok(BaseResponse.ok(null));
    }
}
