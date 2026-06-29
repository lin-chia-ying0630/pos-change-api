package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainPolicyRide {
    private String policyNo;
    private Integer policySeq;
    private String rideType;
    private String rideOrder;
    private String productCode;
    private Integer policyYears;
    private BigDecimal insuredAmount;
    private BigDecimal premium;
}
