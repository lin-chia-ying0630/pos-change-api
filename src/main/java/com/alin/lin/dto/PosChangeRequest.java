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
public class PosChangeRequest {
    private String storeId;
    private String terminalId;
    private BigDecimal changeAmount;
    private String reason;
}
