package com.alin.lin.service.impl;

import com.alin.lin.dao.PosChangeDao;
import com.alin.lin.dto.PosChangeRequest;
import com.alin.lin.entity.PosChange;
import com.alin.lin.service.PosChangeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PosChangeServiceImpl implements PosChangeService {
    private final PosChangeDao posChangeDao;

    public PosChangeServiceImpl(PosChangeDao posChangeDao) {
        this.posChangeDao = posChangeDao;
    }

    @Override
    public List<PosChange> findAll() {
        return posChangeDao.findAll();
    }

    @Override
    public PosChange findById(Long id) {
        PosChange posChange = posChangeDao.findById(id);
        if (posChange == null) {
            throw new NoSuchElementException("pos change not found: " + id);
        }
        return posChange;
    }

    @Override
    @Transactional
    public PosChange create(PosChangeRequest request) {
        PosChange posChange = PosChange.builder()
                .storeId(request.getStoreId())
                .terminalId(request.getTerminalId())
                .changeAmount(request.getChangeAmount())
                .reason(request.getReason())
                .build();
        posChangeDao.insert(posChange);
        return findById(posChange.getId());
    }

    @Override
    @Transactional
    public PosChange update(Long id, PosChangeRequest request) {
        PosChange posChange = findById(id);
        posChange.setStoreId(request.getStoreId());
        posChange.setTerminalId(request.getTerminalId());
        posChange.setChangeAmount(request.getChangeAmount());
        posChange.setReason(request.getReason());
        posChangeDao.update(posChange);
        return findById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!posChangeDao.deleteById(id)) {
            throw new NoSuchElementException("pos change not found: " + id);
        }
    }
}
