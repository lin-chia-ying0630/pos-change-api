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
public class PolicyChangeItem {
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private String changeItem;
    private LocalDateTime createdAt;
}
