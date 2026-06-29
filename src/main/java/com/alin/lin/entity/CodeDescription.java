package com.alin.lin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeDescription {
    private String codeGroup;
    private String codeField;
    private String codeBefore;
    private String codeAfter;
    private String codeDescription;
}
