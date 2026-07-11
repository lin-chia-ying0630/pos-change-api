package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChangeCaseDto {
    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 保全變更案號
    private String changeCaseNo;

    // 受理狀態
    private String acceptanceStatus;

    // 保全變更項目
    private String changeItem;
}
