package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideAmountChangeRequest {
    private String rideOrder;
    private BigDecimal insuredAmount;
}
