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
public class AddressChangeRequest {
    // 保單號碼
    @NotBlank(message = "policyNo 不可空白")
    private String policyNo;

    // 保單序號
    @NotNull(message = "policySeq 不可空白")
    private Integer policySeq;

    // 地址型態
    @NotBlank(message = "addressType 不可空白")
    private String addressType;

    // 郵遞區號前 3 碼
    private String zipCode3;

    // 郵遞區號後 3 碼
    private String zipCode2;

    // 地址
    private String fullWidthAddress;

    // email / 電話 / 手機
    private String halfWidthAddress;
}
