package com.alin.lin.mapper;

import com.alin.lin.entity.PosChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PosChangeMapper {
    List<PosChange> findAll();

    PosChange findById(@Param("id") Long id);

    int insert(PosChange posChange);

    int update(PosChange posChange);

    int deleteById(@Param("id") Long id);
}
