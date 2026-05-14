package com.xiaoniu.aftermarket.reimbursement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReimbursementMapper extends BaseMapper<ReimbursementEntity> {

    @Select("""
            SELECT * FROM reimbursement
            WHERE id = #{id} AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    ReimbursementEntity selectByIdForUpdate(@Param("id") Long id);
}
