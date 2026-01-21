package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.service.DocentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocentController {

    private final DocentService docentService;

    @PostMapping("/admin/core/docent")
    public ResponseEntity<BaseResponse<String>> initDocent(
            @RequestBody String lang
    ) {
        docentService.initDocent(lang);
        docentService.getDocent(lang);
        docentService.getDocentJson(lang);
        return ResponseEntity.ok(BaseResponse.ok(null));
    }
}
