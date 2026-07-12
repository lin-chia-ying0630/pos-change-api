package com.alin.lin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pos.security")
public class PosSecurityProperties {
    // 正式環境開啟角色驗證；本機可關閉以方便開發。
    private boolean enabled;

    // 經辦人員帳號與密碼。
    private String makerUsername;
    private String makerPassword;

    // 覆核人員帳號與密碼。
    private String reviewerUsername;
    private String reviewerPassword;
}
