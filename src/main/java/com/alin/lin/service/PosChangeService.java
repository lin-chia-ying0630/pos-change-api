package com.alin.lin.service;

import com.alin.lin.dto.PosChangeRequest;
import com.alin.lin.entity.PosChange;

import java.util.List;

public interface PosChangeService {
    List<PosChange> findAll();

    PosChange findById(Long id);

    PosChange create(PosChangeRequest request);

    PosChange update(Long id, PosChangeRequest request);

    void delete(Long id);
}
