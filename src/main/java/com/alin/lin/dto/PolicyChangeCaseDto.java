package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChangeCaseDto {
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private String acceptanceStatus;
    private String changeItems;
    private String changeItemDescriptions;
}
