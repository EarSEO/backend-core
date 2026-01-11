package com.earseo.core.service.master;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public enum TourApiPath {
    KoArea("/B551011/KorService2/areaBasedList2"),
    EnArea("/B551011/EngService2/areaBasedList2"),
    KoCommon("/B551011/KorService2/detailCommon2"),
    EnCommon("/B551011/EngService2/detailCommon2"),
    KoDetail("/B551011/KorService2/detailIntro2"),
    EnDetail("/B551011/EngService2/detailIntro2");

    private String path;
}
