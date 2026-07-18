package com.alin.lin.service;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.service.impl.ChangeCaseReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeCaseReviewServiceImplTest {
    private PolicyChangeDao dao;
    private CodeDescriptionService codeService;
    private ChangeCaseApplyService applyService;
    private CurrentUserService currentUserService;
    private ChangeCaseReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        dao = mock(PolicyChangeDao.class);
        PolicyChangeSupportService supportService = mock(PolicyChangeSupportService.class);
        codeService = mock(CodeDescriptionService.class);
        applyService = mock(ChangeCaseApplyService.class);
        currentUserService = mock(CurrentUserService.class);
        PosChangeProperties properties = new PosChangeProperties();
        properties.setZoneId("Asia/Taipei");
        service = new ChangeCaseReviewServiceImpl(
                dao, supportService, codeService, applyService, currentUserService, properties
        );

        when(currentUserService.securityEnabled()).thenReturn(true);
        when(currentUserService.hasRole("REVIEWER")).thenReturn(true);
        when(currentUserService.username()).thenReturn("reviewer-a");
        when(codeService.pendingStatusCode()).thenReturn("P");
        when(codeService.processingStatusCode()).thenReturn("A");
        when(codeService.completeStatusCode()).thenReturn("S");
        when(codeService.cancelStatusCode()).thenReturn("C");
    }

    @Test
    void rejectsSelfReview() {
        when(dao.findChangeCase("P000000001", 1, "C1150718001")).thenReturn(
                PolicyChangeCaseDto.builder()
                        .changeCaseNo("C1150718001")
                        .acceptanceStatus("P")
                        .createdBy("reviewer-a")
                        .build()
        );

        assertThatThrownBy(() -> service.updateChangeCaseStatus("C1150718001", completeRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("不可覆核自己的案件");
    }

    @Test
    void recordsReviewerAndReviewTimeWhenCompletingCase() {
        when(dao.findChangeCase("P000000001", 1, "C1150718001")).thenReturn(
                PolicyChangeCaseDto.builder()
                        .changeCaseNo("C1150718001")
                        .acceptanceStatus("P")
                        .createdBy("maker-a")
                        .build()
        );
        when(dao.updateAcceptanceStatusIfCurrent(any(), anyString())).thenReturn(1);
        when(applyService.applyChangeCase("P000000001", 1, "C1150718001")).thenReturn(1);

        service.updateChangeCaseStatus("C1150718001", completeRequest());

        ArgumentCaptor<PolicyChangeAcceptance> acceptanceCaptor = ArgumentCaptor.forClass(PolicyChangeAcceptance.class);
        verify(dao, org.mockito.Mockito.times(2)).updateAcceptanceStatusIfCurrent(acceptanceCaptor.capture(), anyString());
        List<PolicyChangeAcceptance> transitions = acceptanceCaptor.getAllValues();
        assertThat(transitions.get(0).getAcceptanceStatus()).isEqualTo("A");
        assertThat(transitions.get(0).getReviewedBy()).isNull();
        assertThat(transitions.get(1).getAcceptanceStatus()).isEqualTo("S");
        assertThat(transitions.get(1).getReviewedBy()).isEqualTo("reviewer-a");
        assertThat(transitions.get(1).getReviewedAt()).isNotNull();
    }

    private UpdateChangeCaseStatusRequest completeRequest() {
        return UpdateChangeCaseStatusRequest.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .acceptanceStatus("S")
                .build();
    }
}
