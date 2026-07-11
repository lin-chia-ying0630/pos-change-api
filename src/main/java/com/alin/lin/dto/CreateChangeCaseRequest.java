package com.alin.lin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChangeCaseRequest {
    // 保單號碼
    @NotBlank(message = "policyNo 不可空白")
    private String policyNo;

    // 保單序號
    @NotNull(message = "policySeq 不可空白")
    private Integer policySeq;

    // 保全變更項目
    @NotBlank(message = "changeItem 不可空白")
    private String changeItem;
}
