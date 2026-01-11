package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.etl.AreaItemDto;
import com.earseo.core.service.MasterDataService;
import com.earseo.core.service.master.TourApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/admin/core/master/ko")
    public ResponseEntity<BaseResponse<String>> createTableKo() throws IOException {
        List<AreaItemDto> list = masterDataService.getTourApiArea(TourApiPath.KoArea.getPath());
        masterDataService.createMasterTable(list, TourApiPath.KoCommon.getPath(), TourApiPath.KoDetail.getPath(), "ko");
        return ResponseEntity.ok(BaseResponse.ok(null));
    }

    @GetMapping("/admin/core/master/en")
    public ResponseEntity<BaseResponse<String>> createTableEn() throws IOException {
        List<AreaItemDto> list = masterDataService.getTourApiArea(TourApiPath.EnArea.getPath());
        masterDataService.createMasterTable(list, TourApiPath.EnCommon.getPath(), TourApiPath.EnDetail.getPath(), "en");
        return ResponseEntity.ok(BaseResponse.ok(null));
    }

    @GetMapping("/admin/core/init")
    public ResponseEntity<BaseResponse<String>> init(){
        masterDataService.createCategory();
        return ResponseEntity.ok(BaseResponse.ok(null));
    }
}
