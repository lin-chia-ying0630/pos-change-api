package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBodyDto<T> {
    private boolean success;
    @Builder.Default
    private String message = "";
    @Builder.Default
    private String massageCode = "";
    @Builder.Default
    private String errorMessage = "";
    private T data;
}
