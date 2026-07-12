package com.alin.lin.controller;

import com.alin.lin.config.PosSecurityProperties;
import com.alin.lin.dto.CurrentUserDto;
import com.alin.lin.dto.ResponseBodyDto;
import com.alin.lin.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PosSecurityProperties securityProperties;

    public AuthController(PosSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    // 畫面對應：登入頁驗證帳號，並取得可操作的經辦或覆核角色。
    @GetMapping("/me")
    public ResponseEntity<ResponseBodyDto<CurrentUserDto>> currentUser(Authentication authentication) {
        if (!securityProperties.isEnabled()) {
            return ResponseUtil.ok(CurrentUserDto.builder()
                    .username("local-development")
                    .roles(List.of("MAKER", "REVIEWER"))
                    .securityEnabled(false)
                    .build());
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        return ResponseUtil.ok(CurrentUserDto.builder()
                .username(authentication.getName())
                .roles(roles)
                .securityEnabled(true)
                .build());
    }
}
