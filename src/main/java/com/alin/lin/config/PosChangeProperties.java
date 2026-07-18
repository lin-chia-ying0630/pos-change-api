package com.alin.lin.config;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "pos.change-case")
public class PosChangeProperties {
    // 案號日期時區
    @NotBlank
    private String zoneId;

    // 案號保留期限，逾期案號不得建立受理資料。
    @NotNull
    private Duration reservationTtl;
}
