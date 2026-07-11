package com.alin.lin.service;

import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.RiderAmountChangeListRequest;

public interface AmountChangeSaveService {
    MainAmountChangeDto saveMainAmountChange(String changeCaseNo, MainAmountChangeRequest request);

    MainAmountChangeDto saveRiderAmountChange(String changeCaseNo, String policyNo, Integer policySeq, RiderAmountChangeListRequest request);
}
