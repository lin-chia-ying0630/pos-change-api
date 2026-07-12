package com.alin.lin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "pos.cors")
public class PosCorsProperties {
    // 允許呼叫 API 的前端來源。
    private List<String> allowedOrigins = new ArrayList<>();
}
