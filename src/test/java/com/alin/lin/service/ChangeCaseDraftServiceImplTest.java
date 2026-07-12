package com.alin.lin.service;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.service.impl.ChangeCaseDraftServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeCaseDraftServiceImplTest {
    @Test
    void reservesDatabaseSequenceAndAllowsMoreThanThreeSerialDigits() {
        PolicyChangeDao dao = mock(PolicyChangeDao.class);
        PolicyChangeSupportService supportService = mock(PolicyChangeSupportService.class);
        CodeDescriptionService codeService = mock(CodeDescriptionService.class);
        PosChangeProperties properties = new PosChangeProperties();
        properties.setZoneId("Asia/Taipei");
        when(dao.incrementCaseSequence(any(LocalDate.class))).thenReturn(2);
        when(dao.findLastInsertedSequence()).thenReturn(1000L);
        when(codeService.pendingStatusCode()).thenReturn("P");

        ChangeCaseDraftServiceImpl service = new ChangeCaseDraftServiceImpl(dao, supportService, codeService, properties);
        CreateChangeCaseDto result = service.createChangeCase(CreateChangeCaseRequest.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .changeItem("001")
                .build());

        assertTrue(result.getChangeCaseNo().endsWith("1000"));
        assertEquals("P", result.getAcceptanceStatus());
        verify(dao).incrementCaseSequence(any(LocalDate.class));
        verify(dao).findLastInsertedSequence();
    }
}
