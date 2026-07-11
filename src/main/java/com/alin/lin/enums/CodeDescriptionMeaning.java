package com.alin.lin.enums;

import lombok.Getter;

@Getter
public enum CodeDescriptionMeaning {
    // 通訊地址
    COMMUNICATION_ADDRESS(CodeTable.ADDRESS_TYPE, "01"),

    // 戶籍地址
    REGISTERED_ADDRESS(CodeTable.ADDRESS_TYPE, "02"),

    // email
    EMAIL_ADDRESS(CodeTable.ADDRESS_TYPE, "31"),

    // 地址變更
    ADDRESS_CHANGE(CodeTable.CHANGE_ITEM, "001"),

    // 主約保額變更
    MAIN_AMOUNT_CHANGE(CodeTable.CHANGE_ITEM, "002"),

    // 附約保額變更
    RIDER_AMOUNT_CHANGE(CodeTable.CHANGE_ITEM, "003"),

    // 受理中
    PENDING_STATUS(CodeTable.ACCEPTANCE_STATUS, "P"),

    // 完成
    COMPLETE_STATUS(CodeTable.ACCEPTANCE_STATUS, "S"),

    // 取消
    CANCEL_STATUS(CodeTable.ACCEPTANCE_STATUS, "C"),

    // 主約
    MAIN_RIDE_TYPE(CodeTable.RIDE_TYPE, "1");

    private final CodeTable codeTable;
    private final String codeBefore;

    CodeDescriptionMeaning(CodeTable codeTable, String codeBefore) {
        this.codeTable = codeTable;
        this.codeBefore = codeBefore;
    }
}
