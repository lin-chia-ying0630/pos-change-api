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
public class MainAmountChangeRequest {
    // 保單號碼
    @NotBlank(message = "policyNo 不可空白")
    private String policyNo;

    // 保單序號
    @NotNull(message = "policySeq 不可空白")
    private Integer policySeq;

    // 變更後主約保額
    @NotNull(message = "masterInsuredAmount 不可空白")
    private BigDecimal masterInsuredAmount;
}
