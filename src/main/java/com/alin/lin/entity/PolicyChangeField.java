package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChangeField {
    private Long id;
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private String changeItem;
    private String changeField;
    private String changeKey;
    private String contentBefore;
    private String contentAfter;
}
