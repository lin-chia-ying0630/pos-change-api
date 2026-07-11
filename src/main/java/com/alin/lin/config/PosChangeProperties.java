package com.alin.lin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pos.change-case")
public class PosChangeProperties {
    // 案號日期時區
    private String zoneId;
}
