package com.earseo.core.service.master;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryGroup {

    NA("NA", "자연", "Nature"),
    CU("CU", "문화", "Culture"),
    AR("AR", "예술", "Art"),
    HI("HI", "역사", "History"),
    LS("LS", "레포츠", "Leisure/Sports"),
    SH("SH", "쇼핑", "Shopping");

    private final String code;
    private final String koName;
    private final String enName;
}
