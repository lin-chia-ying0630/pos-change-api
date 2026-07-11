package com.alin.lin.service.impl;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.service.ChangeCaseDraftService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class ChangeCaseDraftServiceImpl implements ChangeCaseDraftService {
    private static final int SERIAL_MAX = 999;
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;
    private final ZoneId changeCaseZoneId;
    private String currentCaseNoPrefix;
    private int currentCaseNoSerial;

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

    private synchronized String generateChangeCaseNo() {
        String prefix = buildTodayCaseNoPrefix();
        if (!prefix.equals(currentCaseNoPrefix)) {
            currentCaseNoPrefix = prefix;
            currentCaseNoSerial = findLatestSerial(prefix);
        }

        if (currentCaseNoSerial >= SERIAL_MAX) {
            throw new IllegalStateException("今日變更案號流水號已達上限");
        }

        currentCaseNoSerial++;
        return prefix + String.format("%03d", currentCaseNoSerial);
    }

    private String buildTodayCaseNoPrefix() {
        LocalDate today = LocalDate.now(changeCaseZoneId);
        int rocYear = today.getYear() - 1911;
        return "C" + String.format("%03d", rocYear) + today.format(MONTH_DAY_FORMATTER);
    }

    private int findLatestSerial(String prefix) {
        String maxChangeCaseNo = policyChangeDao.findMaxChangeCaseNoByPrefix(prefix);
        if (maxChangeCaseNo == null || maxChangeCaseNo.length() <= prefix.length()) {
            return 0;
        }
        return Integer.parseInt(maxChangeCaseNo.substring(prefix.length()));
    }
}
