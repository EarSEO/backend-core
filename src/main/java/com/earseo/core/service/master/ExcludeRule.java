package com.earseo.core.service.master;

import com.earseo.core.dto.tourApi.AreaResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum ExcludeRule {

    CONTENT_TYPE_ID("25", "32", "39"),
    CONTENT_TYPE_ID_EN("80","82","77"),
    CAT3("A04011000");

    private final Set<String> rules;

    ExcludeRule(String... rules) {
        this.rules = Set.of(rules);
    }

    public static boolean shouldExclude(AreaResponse.Item item) {
        return CONTENT_TYPE_ID.rules.contains(String.valueOf(item.contenttypeid()))
                || CONTENT_TYPE_ID_EN.rules.contains(String.valueOf(item.contenttypeid()))
                || CAT3.rules.contains(String.valueOf(item.cat3()));
    }

    public static boolean isInclude(AreaResponse.Item item) {
        return !shouldExclude(item);
    }

}
