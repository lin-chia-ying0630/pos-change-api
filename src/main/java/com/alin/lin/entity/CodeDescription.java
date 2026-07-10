package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeDescription {
    // 代碼群組
    private String codeGroup;

    // 代碼欄位
    private String codeField;

    // 轉換前代碼
    private String codeBefore;

    // 轉換後代碼
    private String codeAfter;

    // 代碼中文或英文說明
    private String codeDescription;
}
