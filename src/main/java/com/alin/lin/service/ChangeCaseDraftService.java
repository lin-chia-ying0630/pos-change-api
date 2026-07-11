package com.alin.lin.service;

import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;

public interface ChangeCaseDraftService {
    CreateChangeCaseDto createChangeCase(CreateChangeCaseRequest request);
}
