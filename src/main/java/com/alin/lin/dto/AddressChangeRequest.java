package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressChangeRequest {
    private String policyNo;
    private Integer policySeq;
    private String addressType;
    private String zipCode3;
    private String zipCode2;
    private String fullWidthAddress;
    private String halfWidthAddress;
}
