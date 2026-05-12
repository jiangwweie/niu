package com.xiaoniu.aftermarket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.payment.entity.PaymentRecordEntity;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_record
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            """)
    BigDecimal sumAmountByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
