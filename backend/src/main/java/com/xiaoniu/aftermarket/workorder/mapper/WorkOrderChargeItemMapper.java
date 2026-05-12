package com.xiaoniu.aftermarket.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkOrderChargeItemMapper extends BaseMapper<WorkOrderChargeItemEntity> {

    @Select("""
            SELECT * FROM work_order_charge_item
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            ORDER BY id ASC
            """)
    List<WorkOrderChargeItemEntity> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
