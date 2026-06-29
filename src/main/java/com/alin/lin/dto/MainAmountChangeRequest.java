package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainAmountChangeRequest {
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private BigDecimal masterInsuredAmount;
    private List<RideAmountChangeRequest> rides;
}
