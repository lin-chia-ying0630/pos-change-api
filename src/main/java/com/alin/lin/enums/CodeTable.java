package com.alin.lin.enums;

import lombok.Getter;

@Getter
public enum CodeTable {
    // 地址型態
    ADDRESS_TYPE("main-policy-address", "address_type"),

    // 附約型態
    RIDE_TYPE("main-policy-ride", "ride_type"),

    // 保全受理狀態
    ACCEPTANCE_STATUS("policy-change-acceptance", "acceptance_status"),

    // 保全變更項目
    CHANGE_ITEM("policy-change-item", "change_item"),

    // 郵遞區號前三碼對應縣市區
    POSTAL_CODE_ZIP_CODE3("postal-code", "zip_code3");

    private final String codeGroup;
    private final String codeField;

    CodeTable(String codeGroup, String codeField) {
        this.codeGroup = codeGroup;
        this.codeField = codeField;
    }
}
