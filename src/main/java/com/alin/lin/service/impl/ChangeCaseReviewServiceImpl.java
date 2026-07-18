package com.alin.lin.service.impl;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.exception.ChangeCaseConflictException;
import com.alin.lin.service.ChangeCaseApplyService;
import com.alin.lin.service.ChangeCaseReviewService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.CurrentUserService;
import com.alin.lin.service.PolicyChangeSupportService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotNull;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class ChangeCaseReviewServiceImpl implements ChangeCaseReviewService {
    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;
    private final ChangeCaseApplyService changeCaseApplyService;
    private final CurrentUserService currentUserService;
    private final ZoneId changeCaseZoneId;

    public ChangeCaseReviewServiceImpl(
            PolicyChangeDao policyChangeDao,
            PolicyChangeSupportService policyChangeSupportService,
            CodeDescriptionService codeDescriptionService,
            ChangeCaseApplyService changeCaseApplyService,
            CurrentUserService currentUserService,
            PosChangeProperties posChangeProperties
    ) {
        this.policyChangeDao = policyChangeDao;
        this.policyChangeSupportService = policyChangeSupportService;
        this.codeDescriptionService = codeDescriptionService;
        this.changeCaseApplyService = changeCaseApplyService;
        this.currentUserService = currentUserService;
        this.changeCaseZoneId = ZoneId.of(posChangeProperties.getZoneId());
    }

    @Override
    @Transactional
    public UpdateChangeCaseStatusDto updateChangeCaseStatus(String changeCaseNo, UpdateChangeCaseStatusRequest request) {
        requireText(changeCaseNo, "changeCaseNo");
        requireText(request.getPolicyNo(), "policyNo");
        requireNotNull(request.getPolicySeq(), "policySeq");
        policyChangeSupportService.requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        if (currentUserService.securityEnabled() && !currentUserService.hasRole("REVIEWER")) {
            throw new AccessDeniedException("只有覆核人員可以變更案件狀態");
        }

        String targetStatus = normalizeStatus(request.getAcceptanceStatus());
        String pendingStatus = codeDescriptionService.pendingStatusCode();
        if (!codeDescriptionService.cancelStatusCode().equals(targetStatus)
                && !codeDescriptionService.completeStatusCode().equals(targetStatus)) {
            throw new IllegalArgumentException("受理狀態只能改為 C 或 S");
        }

        PolicyChangeCaseDto changeCase = policyChangeDao.findChangeCase(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo
        );
        if (changeCase == null) {
            throw new NoSuchElementException("找不到保全受理資料: " + changeCaseNo);
        }
        if (currentUserService.securityEnabled()
                && Objects.equals(currentUserService.username(), changeCase.getCreatedBy())) {
            throw new AccessDeniedException("建檔經辦不可覆核自己的案件");
        }
        if (!pendingStatus.equals(normalizeStatus(changeCase.getAcceptanceStatus()))) {
            throw new ChangeCaseConflictException("案件已由其他人處理，只有 P-受理中可以覆核");
        }

        int appliedItemCount = 0;
        if (codeDescriptionService.cancelStatusCode().equals(targetStatus)) {
            changeStatusOrThrow(request, changeCaseNo, pendingStatus, targetStatus);
        } else {
            String processingStatus = codeDescriptionService.processingStatusCode();
            changeStatusOrThrow(request, changeCaseNo, pendingStatus, processingStatus);
            appliedItemCount = changeCaseApplyService.applyChangeCase(
                    request.getPolicyNo(), request.getPolicySeq(), changeCaseNo
            );
            changeStatusOrThrow(request, changeCaseNo, processingStatus, targetStatus);
        }

        return UpdateChangeCaseStatusDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(targetStatus)
                .appliedItemCount(appliedItemCount)
                .build();
    }

    private void changeStatusOrThrow(
            UpdateChangeCaseStatusRequest request,
            String changeCaseNo,
            String expectedStatus,
            String targetStatus
    ) {
        int updated = policyChangeDao.updateAcceptanceStatusIfCurrent(
                PolicyChangeAcceptance.builder()
                        .policyNo(request.getPolicyNo())
                        .policySeq(request.getPolicySeq())
                        .changeCaseNo(changeCaseNo)
                        .acceptanceStatus(targetStatus)
                        .reviewedBy(isFinalStatus(targetStatus) ? currentUserService.username() : null)
                        .reviewedAt(isFinalStatus(targetStatus) ? LocalDateTime.now(changeCaseZoneId) : null)
                        .build(),
                expectedStatus
        );
        if (updated != 1) {
            throw new ChangeCaseConflictException("案件狀態已變更，請重新查詢後再覆核");
        }
    }

    private boolean isFinalStatus(String status) {
        return codeDescriptionService.completeStatusCode().equals(status)
                || codeDescriptionService.cancelStatusCode().equals(status);
    }

    private String normalizeStatus(String acceptanceStatus) {
        requireText(acceptanceStatus, "acceptanceStatus");
        return acceptanceStatus.trim().toUpperCase();
    }
}
