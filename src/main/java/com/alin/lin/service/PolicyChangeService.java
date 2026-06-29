package com.alin.lin.service;

import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;

import java.util.List;

public interface PolicyChangeService {
    PolicyDetailDto findPolicyDetail(String policyNo, Integer policySeq);

    CreateChangeCaseDto createChangeCase(CreateChangeCaseRequest request);

    AddressChangeDto saveAddressChange(AddressChangeRequest request);

    MainAmountChangeDto saveMainAmountChange(MainAmountChangeRequest request);

    MainAmountChangeDto saveRiderAmountChange(RiderAmountChangeListRequest request);

    List<PolicyChangeCaseDto> findChangeCases(String policyNo);

    UpdateChangeCaseStatusDto updateChangeCaseStatus(String changeCaseNo, UpdateChangeCaseStatusRequest request);
}
