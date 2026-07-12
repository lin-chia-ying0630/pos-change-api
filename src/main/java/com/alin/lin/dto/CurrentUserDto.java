package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserDto {
    // 登入帳號。
    private String username;

    // 使用者角色，例如 MAKER 或 REVIEWER。
    private List<String> roles;

    // 是否啟用正式權限驗證。
    private boolean securityEnabled;
}
