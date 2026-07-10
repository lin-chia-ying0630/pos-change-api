package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainPolicyAddress {
    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 地址型態
    private String addressType;

    // 郵遞區號前 3 碼
    private String zipCode3;

    // 郵遞區號後 3 碼
    private String zipCode2;

    // 地址全型
    private String fullWidthAddress;

    // 地址半型
    private String halfWidthAddress;

    // 建立時間
    private LocalDateTime createdAt;

    // 更新時間
    private LocalDateTime updatedAt;
}
