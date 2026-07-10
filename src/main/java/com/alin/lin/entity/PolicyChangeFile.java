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
public class PolicyChangeFile {
    private Long id;
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private String changeItem;
    private String changeFile;
    private String contentBefore;
    private String contentAfter;
    private LocalDateTime createdAt;
}
