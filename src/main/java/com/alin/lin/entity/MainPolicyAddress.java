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
    private String policyNo;
    private Integer policySeq;
    private String addressType;
    private String zipCode3;
    private String zipCode2;
    private String fullWidthAddress;
    private String halfWidthAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
