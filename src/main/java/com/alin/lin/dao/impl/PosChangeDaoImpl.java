package com.alin.lin.dao.impl;

import com.alin.lin.dao.PosChangeDao;
import com.alin.lin.entity.PosChange;
import com.alin.lin.mapper.PosChangeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PosChangeDaoImpl implements PosChangeDao {
    private final PosChangeMapper posChangeMapper;

    public PosChangeDaoImpl(PosChangeMapper posChangeMapper) {
        this.posChangeMapper = posChangeMapper;
    }

    @Override
    public List<PosChange> findAll() {
        return posChangeMapper.findAll();
    }

    @Override
    public PosChange findById(Long id) {
        return posChangeMapper.findById(id);
    }

    @Override
    public void insert(PosChange posChange) {
        posChangeMapper.insert(posChange);
    }

    @Override
    public void update(PosChange posChange) {
        posChangeMapper.update(posChange);
    }

    @Override
    public boolean deleteById(Long id) {
        return posChangeMapper.deleteById(id) > 0;
    }
}
