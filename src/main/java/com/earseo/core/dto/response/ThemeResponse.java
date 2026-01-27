package com.earseo.core.dto.response;

import com.earseo.core.service.master.CategoryGroup;
import com.earseo.core.service.master.SubCategoryGroup;

public record ThemeResponse(
        String code,
        String koName,
        String enName
) {

    public static ThemeResponse fromSub(SubCategoryGroup subCategoryGroup) {
        return new ThemeResponse(subCategoryGroup.getCode(), subCategoryGroup.getKoName(), subCategoryGroup.getEnName());
    }

    public static ThemeResponse from(CategoryGroup categoryGroup) {
        return new ThemeResponse(categoryGroup.getCode(), categoryGroup.getKoName(), categoryGroup.getEnName());
    }
}