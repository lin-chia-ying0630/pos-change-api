package com.alin.lin.controller;

import com.alin.lin.entity.CodeDescription;
import com.alin.lin.dto.ResponseBodyDto;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user-authorizations")
public class UserAuthorizationController {
    private final CodeDescriptionService codeDescriptionService;

    public UserAuthorizationController(CodeDescriptionService codeDescriptionService) {
        this.codeDescriptionService = codeDescriptionService;
    }

    // 畫面對應：使用者授權頁顯示新增、修改、刪除、覆核與角色對照。
    @GetMapping
    public ResponseEntity<ResponseBodyDto<List<CodeDescription>>> findPermissions() {
        return ResponseUtil.ok(codeDescriptionService.findUserAuthorizationPermissions());
    }
}
