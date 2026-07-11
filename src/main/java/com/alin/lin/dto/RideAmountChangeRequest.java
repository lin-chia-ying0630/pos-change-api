package com.alin.lin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    // 主附約序號
    @NotBlank(message = "rideOrder 不可空白")
    private String rideOrder;

    // 變更後保額
    @NotNull(message = "insuredAmount 不可空白")
    private BigDecimal insuredAmount;
}
