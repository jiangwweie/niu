package com.xiaoniu.aftermarket.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrderEntity> {

    @Select("""
            SELECT * FROM work_order
            WHERE id = #{id} AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    WorkOrderEntity selectByIdForUpdate(@Param("id") Long id);
}
