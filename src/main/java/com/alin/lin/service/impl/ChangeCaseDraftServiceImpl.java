package com.alin.lin.service.impl;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.service.ChangeCaseDraftService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class ChangeCaseDraftServiceImpl implements ChangeCaseDraftService {
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;
    private final ZoneId changeCaseZoneId;

    public ChangeCaseDraftServiceImpl(
            PolicyChangeDao policyChangeDao,
            PolicyChangeSupportService policyChangeSupportService,
            CodeDescriptionService codeDescriptionService,
            PosChangeProperties posChangeProperties
    ) {
        this.policyChangeDao = policyChangeDao;
        this.policyChangeSupportService = policyChangeSupportService;
        this.codeDescriptionService = codeDescriptionService;
        this.changeCaseZoneId = ZoneId.of(posChangeProperties.getZoneId());
    }

    @Override
    @Transactional
    public CreateChangeCaseDto createChangeCase(CreateChangeCaseRequest request) {
        policyChangeSupportService.requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(request.getChangeItem(), "changeItem");

        String changeCaseNo = generateChangeCaseNo();

        return CreateChangeCaseDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(codeDescriptionService.pendingStatusCode())
                .changeItem(request.getChangeItem())
                .build();
    }

    private String generateChangeCaseNo() {
        LocalDate today = LocalDate.now(changeCaseZoneId);
        if (policyChangeDao.incrementCaseSequence(today) < 1) {
            throw new IllegalStateException("更新變更案號流水號失敗");
        }
        Long nextSerial = policyChangeDao.findLastInsertedSequence();
        if (nextSerial == null || nextSerial < 1) {
            throw new IllegalStateException("無法取得今日變更案號流水號");
        }
        return buildCaseNoPrefix(today) + String.format("%03d", nextSerial);
    }

    private String buildCaseNoPrefix(LocalDate today) {
        int rocYear = today.getYear() - 1911;
        return "C" + String.format("%03d", rocYear) + today.format(MONTH_DAY_FORMATTER);
    }
}
