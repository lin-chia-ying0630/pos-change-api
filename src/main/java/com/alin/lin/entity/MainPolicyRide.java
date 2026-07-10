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
public class MainPolicyRide {
    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 主附約型態
    private String rideType;

    // 主附約序號
    private String rideOrder;

    // 險種代碼
    private String productCode;

    // 年期
    private Integer policyYears;

    // 保額
    private BigDecimal insuredAmount;

    // 保費
    private BigDecimal premium;

    // 建立時間
    private LocalDateTime createdAt;

    // 更新時間
    private LocalDateTime updatedAt;
}
