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
public class PolicyChangeCaseReservation {
    // 保全變更案號
    private String changeCaseNo;

    // 保單號碼
    private String policyNo;

    // 保單序號
    private Integer policySeq;

    // 領取案號的經辦帳號
    private String reservedBy;

    // 案號有效期限
    private LocalDateTime expiresAt;

    // 案號建立受理資料的時間
    private LocalDateTime consumedAt;

    // 建立時間
    private LocalDateTime createdAt;
}
