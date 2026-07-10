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
    private String policyNo;
    private Integer policySeq;
    private String mainProductCode;
    private Integer mainPolicyYears;
    private BigDecimal insuredAmount;
    private BigDecimal premium;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
