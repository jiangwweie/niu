package com.xiaoniu.aftermarket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.payment.entity.RefundRecordEntity;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecordEntity> {

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM refund_record
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            """)
    BigDecimal sumAmountByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT * FROM refund_record
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            ORDER BY refunded_at ASC, id ASC
            """)
    List<RefundRecordEntity> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
