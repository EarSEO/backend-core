package com.earseo.core.service.master;

import com.earseo.core.entity.Category;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum SubCategoryGroup {

    NA01(CategoryGroup.NA, "NA01", "자연관광지", "Natural Sites", List.of("A0101")),
    NA02(CategoryGroup.NA, "NA02", "관광자원", "Natural Resources", List.of("A0102")),

    CU01(CategoryGroup.CU, "CU01", "휴양관광지", "Recreational Sites", List.of("A0202")),
    CU02(CategoryGroup.CU, "CU02", "체험관광지", "Experience Programs", List.of("A0203")),
    CU03(CategoryGroup.CU, "CU03", "산업관광지", "Industrial Sites", List.of("A0204")),
    CU04(CategoryGroup.CU, "CU04", "문화시설", "Cultural Facilities", List.of("A0206")),

    AR01(CategoryGroup.AR, "AR01", "축제", "Festivals", List.of("A0207")),
    AR02(CategoryGroup.AR, "AR02", "공연/행사", "Events/Performances", List.of("A0208")),

    HI01(CategoryGroup.HI, "HI01", "역사관광지", "Historical Sites", List.of("A0201")),
    HI02(CategoryGroup.HI, "HI02", "건축/조형물", "Architectural Sights", List.of("A0205")),

    LS01(CategoryGroup.LS, "LS01", "레포츠소개", "Introduction", List.of("A0301")),
    LS02(CategoryGroup.LS, "LS02", "육상 레포츠", "Land Sports", List.of("A0302")),
    LS03(CategoryGroup.LS, "LS03", "수상 레포츠", "Water Sports", List.of("A0303")),
    LS04(CategoryGroup.LS, "LS04", "항공 레포츠", "Sky Sports", List.of("A0304")),
    LS05(CategoryGroup.LS, "LS05", "복합 레포츠", "Others", List.of("A0305")),

    SH01(CategoryGroup.SH, "SH01", "쇼핑", "Shopping", List.of("A0401"));

    private final CategoryGroup categoryGroup;
    private final String code;
    private final String koName;
    private final String enName;
    private final List<String> originPrefixes;

    public static Optional<SubCategoryGroup> from(Category category) {
        return Arrays.stream(values())
                .filter(st ->
                        st.originPrefixes.stream()
                                .anyMatch(prefix -> category.getCode().startsWith(prefix))
                )
                .findFirst();
    }
    public static SubCategoryGroup fromCode(String code) {
        return Arrays.stream(values())
                .filter(st ->
                        st.originPrefixes.stream()
                                .anyMatch(prefix -> code.startsWith(prefix))
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("카테고리 파싱 에러 : " + code)
                );
    }
}
