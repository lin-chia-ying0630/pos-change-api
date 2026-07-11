package com.alin.lin.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum PolicyChangeFieldName {
    // 保單主檔主約險種
    MAIN_PRODUCT_CODE("main_product_code"),

    // 保單主檔主約年期
    MAIN_POLICY_YEARS("main_policy_years"),

    // 保單主檔保額欄位
    INSURED_AMOUNT("insured_amount"),

    // 保單主檔完整保額異動欄位
    MASTER_INSURED_AMOUNT("main_policy_master.insured_amount"),

    // 主檔異動 key
    MASTER_CHANGE_KEY("MASTER"),

    // 地址郵遞區號前三碼
    ZIP_CODE3("zip_code3"),

    // 地址郵遞區號後三碼
    ZIP_CODE2("zip_code2"),

    // 地址
    FULL_WIDTH_ADDRESS("full_width_address"),

    // email / 電話 / 手機
    HALF_WIDTH_ADDRESS("half_width_address");

    private static final Set<String> MASTER_CHANGE_FIELD_NAMES = Set.of(
            MAIN_PRODUCT_CODE.fieldName,
            MAIN_POLICY_YEARS.fieldName,
            INSURED_AMOUNT.fieldName
    );

    private final String fieldName;

    PolicyChangeFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static Set<String> masterChangeFieldNames() {
        return MASTER_CHANGE_FIELD_NAMES;
    }
}
