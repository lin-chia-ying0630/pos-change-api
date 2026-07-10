package com.alin.lin.dao;

import com.alin.lin.entity.PosChange;

import java.util.List;

public interface PosChangeDao {
    List<PosChange> findAll();

    PosChange findById(Long id);

    void insert(PosChange posChange);

    void update(PosChange posChange);

    boolean deleteById(Long id);
}
