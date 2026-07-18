package com.alin.lin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChangeCaseRequest {
    // 保單號碼
    @NotBlank(message = "policyNo 不可空白")
    @Pattern(regexp = com.alin.lin.util.ValidationPatterns.POLICY_NO, message = "policyNo 格式錯誤")
    private String policyNo;

    // 保單序號
    @NotNull(message = "policySeq 不可空白")
    @Positive(message = "policySeq 必須大於 0")
    private Integer policySeq;

    // 同一案號要辦理的保全變更項目
    @NotEmpty(message = "changeItems 不可空白")
    @Size(max = 3, message = "changeItems 最多 3 筆")
    private List<
            @NotBlank(message = "changeItem 不可空白")
            @Pattern(regexp = com.alin.lin.util.ValidationPatterns.CHANGE_ITEM, message = "changeItem 格式錯誤")
            String> changeItems;
}
