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
    // 郵遞區號
    private String postalCode;

    // 郵遞區號前 3 碼
    private String zipCode3;

    // 縣市
    private String city;

    // 區鄉鎮市
    private String district;

    // 中文地址前綴
    private String addressPrefix;

    // 英文地址前綴
    private String halfWidthAddressPrefix;
}
