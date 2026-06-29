package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainAmountChangeDto {
    private String changeCaseNo;
    private String changeItem;
    private Integer changedFieldCount;
}
