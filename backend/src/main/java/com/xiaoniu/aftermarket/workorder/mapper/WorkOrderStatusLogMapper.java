package com.xiaoniu.aftermarket.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderStatusLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkOrderStatusLogMapper extends BaseMapper<WorkOrderStatusLogEntity> {

    @Select("""
            SELECT * FROM work_order_status_log
            WHERE work_order_id = #{workOrderId}
            ORDER BY operated_at ASC, id ASC
            """)
    List<WorkOrderStatusLogEntity> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
