package com.alin.lin.controller;

import com.alin.lin.config.PosCorsProperties;
import com.alin.lin.config.PosSecurityProperties;
import com.alin.lin.config.SecurityConfig;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.service.PolicyChangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PolicyChangeController.class, AuthController.class})
@Import(SecurityConfig.class)
@EnableConfigurationProperties({PosSecurityProperties.class, PosCorsProperties.class})
@TestPropertySource(properties = {
        "pos.security.enabled=true",
        "pos.security.maker-username=maker",
        "pos.security.maker-password=maker-secret",
        "pos.security.reviewer-username=reviewer",
        "pos.security.reviewer-password=reviewer-secret",
        "pos.cors.allowed-origins[0]=http://localhost:5173"
})
class SecurityAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyChangeService policyChangeService;

    @MockitoBean
    private PolicyChangeDao policyChangeDao;

    @Test
    void returnsResponseBodyDtoWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/policies/P000000001/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("尚未登入或帳號密碼錯誤"));
    }

    @Test
    void makerCanCreateChangeCase() throws Exception {
        given(policyChangeService.createChangeCase(any())).willReturn(CreateChangeCaseDto.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .changeCaseNo("C1150712001")
                .acceptanceStatus("P")
                .changeItem("001")
                .build());

        mockMvc.perform(post("/api/change-cases")
                        .with(httpBasic("maker", "maker-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyNo": "P000000001",
                                  "policySeq": 1,
                                  "changeItem": "001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeCaseNo").value("C1150712001"));
    }

    @Test
    void reviewerCannotCreateChangeCase() throws Exception {
        mockMvc.perform(post("/api/change-cases")
                        .with(httpBasic("reviewer", "reviewer-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("沒有執行此作業的權限"));
    }

    @Test
    void makerCannotReviewChangeCase() throws Exception {
        mockMvc.perform(patch("/api/change-cases/C1150712001/status")
                        .with(httpBasic("maker", "maker-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("沒有執行此作業的權限"));
    }

    @Test
    void reviewerCanCompleteChangeCase() throws Exception {
        given(policyChangeService.updateChangeCaseStatus(any(), any())).willReturn(UpdateChangeCaseStatusDto.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .changeCaseNo("C1150712001")
                .acceptanceStatus("S")
                .appliedItemCount(1)
                .build());

        mockMvc.perform(patch("/api/change-cases/C1150712001/status")
                        .with(httpBasic("reviewer", "reviewer-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyNo": "P000000001",
                                  "policySeq": 1,
                                  "acceptanceStatus": "S"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.acceptanceStatus").value("S"));
    }

    @Test
    void returnsAuthenticatedReviewerRole() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(httpBasic("reviewer", "reviewer-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("reviewer"))
                .andExpect(jsonPath("$.data.roles[0]").value("REVIEWER"))
                .andExpect(jsonPath("$.data.securityEnabled").value(true));
    }

    @Test
    void allowsConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/policies/P000000001/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
