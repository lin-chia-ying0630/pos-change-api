package com.alin.lin.service;

import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyChangeCaseDetailDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.PostalCodeAreaDto;

import java.util.List;

public interface PolicyQueryService {
    PolicyDetailDto findPolicyDetail(String policyNo, Integer policySeq);

    PostalCodeAreaDto findPostalCodeArea(String postalCode);

    List<PolicyChangeCaseDto> findChangeCases(String policyNo);

    PolicyChangeCaseDetailDto findChangeCaseDetail(String policyNo, Integer policySeq, String changeCaseNo);
}
