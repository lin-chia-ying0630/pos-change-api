package com.alin.lin.service;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyChangeCaseDetailDto;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.service.impl.PolicyQueryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyQueryServiceImplTest {
    @Test
    void splitsSnapshotJsonAndUsesChtCodeFieldName() {
        PolicyChangeDao dao = mock(PolicyChangeDao.class);
        PolicyChangeSupportService supportService = mock(PolicyChangeSupportService.class);
        CodeDescriptionService codeService = mock(CodeDescriptionService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PolicyQueryServiceImpl service = new PolicyQueryServiceImpl(
                dao,
                supportService,
                codeService,
                currentUserService,
                new ObjectMapper()
        );
        when(currentUserService.securityEnabled()).thenReturn(false);

        when(supportService.requirePolicy("P000000001", 1)).thenReturn(new MainPolicyMaster());
        when(dao.findChangeCase("P000000001", 1, "C001")).thenReturn(
                PolicyChangeCaseDto.builder().changeCaseNo("C001").build()
        );
        when(dao.findChangeFieldsByCaseNo("P000000001", 1, "C001")).thenReturn(List.of(
                PolicyChangeField.builder().changeField("half_width_address").build()
        ));
        when(dao.findChangeFilesByCaseNo("P000000001", 1, "C001")).thenReturn(List.of(
                PolicyChangeFile.builder()
                        .id(1L)
                        .contentBefore("{\"policyNo\":\"P000000001\",\"zipCode3\":null}")
                        .contentAfter("{\"policyNo\":\"P000000001\",\"zipCode3\":\"100\"}")
                        .build()
        ));
        when(codeService.findChtFieldNames()).thenReturn(Map.of(
                "policyNo", "保單號碼",
                "zipCode3", "郵遞區號前三碼",
                "halfWidthAddress", "電子郵件／電話／手機"
        ));

        PolicyChangeCaseDetailDto result = service.findChangeCaseDetail("P000000001", 1, "C001");

        assertThat(result.getChangeFiles().get(0).getSnapshotFields())
                .extracting("jsonKey", "chineseName", "contentBefore", "contentAfter")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("policyNo", "保單號碼", "P000000001", "P000000001"),
                        org.assertj.core.groups.Tuple.tuple("zipCode3", "郵遞區號前三碼", null, "100")
                );
        assertThat(result.getChangeFields().get(0).getChineseName())
                .isEqualTo("電子郵件／電話／手機");
    }
}
