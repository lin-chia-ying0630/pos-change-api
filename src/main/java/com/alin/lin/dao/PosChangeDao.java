package com.alin.lin.dao;

import com.alin.lin.entity.PosChange;
import com.alin.lin.mapper.PosChangeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PosChangeDao {
    private final PosChangeMapper posChangeMapper;

    public PosChangeDao(PosChangeMapper posChangeMapper) {
        this.posChangeMapper = posChangeMapper;
    }

    public List<PosChange> findAll() {
        return posChangeMapper.findAll();
    }

    public PosChange findById(Long id) {
        return posChangeMapper.findById(id);
    }

    public void insert(PosChange posChange) {
        posChangeMapper.insert(posChange);
    }

    public void update(PosChange posChange) {
        posChangeMapper.update(posChange);
    }

    public boolean deleteById(Long id) {
        return posChangeMapper.deleteById(id) > 0;
    }
}
