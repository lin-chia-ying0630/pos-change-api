package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainPolicyMaster {
    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 主約險種代碼
    private String mainProductCode;

    // 主約年期
    private Integer mainPolicyYears;

    // 主約保額
    private BigDecimal insuredAmount;

    // 總保費
    private BigDecimal premium;

    // 建立時間
    private LocalDateTime createdAt;

    // 更新時間
    private LocalDateTime updatedAt;
}
