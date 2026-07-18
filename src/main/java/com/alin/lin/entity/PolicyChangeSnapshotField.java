package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChangeSnapshotField {
    // JSON 原始 key。
    private String jsonKey;

    // 從 CHT-code 取得的中文欄位名稱。
    private String chineseName;

    // 異動前值。
    private String contentBefore;

    // 異動後值。
    private String contentAfter;
}
