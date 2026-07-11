package com.alin.lin.enums;

import lombok.Getter;

@Getter
public enum PostalCodeRule {
    // 郵遞區號前三碼
    ZIP_CODE3("\\d{3}"),

    // 郵遞區號前三碼必填，後三碼可空白
    ZIP_CODE_3_OR_6("\\d{3}(\\d{3})?");

    private final String pattern;

    PostalCodeRule(String pattern) {
        this.pattern = pattern;
    }

    public boolean matches(String value) {
        return value != null && value.matches(pattern);
    }
}
