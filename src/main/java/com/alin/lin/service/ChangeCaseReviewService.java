package com.alin.lin.service;

import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;

public interface ChangeCaseReviewService {
    UpdateChangeCaseStatusDto updateChangeCaseStatus(String changeCaseNo, UpdateChangeCaseStatusRequest request);
}
