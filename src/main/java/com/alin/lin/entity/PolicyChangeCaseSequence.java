package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChangeCaseSequence {
    // 流水號所屬日期。
    private LocalDate sequenceDate;

    // 當日目前流水號。
    private Long currentSerial;

    // 建立時間。
    private LocalDateTime createdAt;

    // 最後更新時間。
    private LocalDateTime updatedAt;
}
