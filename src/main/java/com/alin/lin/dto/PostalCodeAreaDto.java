package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostalCodeAreaDto {
    private String postalCode;
    private String zipCode3;
    private String city;
    private String district;
    private String addressPrefix;
    private String halfWidthAddressPrefix;
}
