package com.alin.lin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = com.alin.lin.util.ValidationPatterns.POLICY_NO, message = "policyNo 格式錯誤")
    private String policyNo;

    // 保單序號
    @NotNull(message = "policySeq 不可空白")
    @Positive(message = "policySeq 必須大於 0")
    private Integer policySeq;

    // 地址型態
    @NotBlank(message = "addressType 不可空白")
    @Pattern(regexp = com.alin.lin.util.ValidationPatterns.ADDRESS_TYPE, message = "addressType 必須為 2 碼數字")
    private String addressType;

    // 郵遞區號前 3 碼
    @Pattern(regexp = com.alin.lin.util.ValidationPatterns.ZIP_CODE_PART, message = "zipCode3 必須為 3 碼數字或空白")
    private String zipCode3;

    // 郵遞區號後 3 碼
    @Pattern(regexp = com.alin.lin.util.ValidationPatterns.ZIP_CODE_PART, message = "zipCode2 必須為 3 碼數字或空白")
    private String zipCode2;

    // 地址
    @Size(max = 255, message = "fullWidthAddress 最多 255 個字元")
    private String fullWidthAddress;

    // email / 電話 / 手機
    @Size(max = 255, message = "halfWidthAddress 最多 255 個字元")
    private String halfWidthAddress;
}
