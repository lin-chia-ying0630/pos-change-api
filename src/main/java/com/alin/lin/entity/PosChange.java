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
public class PosChange {
    private Long id;
    private String storeId;
    private String terminalId;
    private BigDecimal changeAmount;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
