package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChangeFile {
    // 流水識別碼
    private Long id;

    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 保全變更案號
    private String changeCaseNo;

    // 保全變更項目
    private String changeItem;

    // 異動檔案名稱
    private String changeFile;

    // 異動前檔案快照
    private String contentBefore;

    // 異動後檔案快照
    private String contentAfter;

    // 建立時間
    private LocalDateTime createdAt;
}
