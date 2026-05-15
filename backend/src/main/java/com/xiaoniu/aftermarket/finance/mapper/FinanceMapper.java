package com.xiaoniu.aftermarket.finance.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinanceMapper {

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_record
            WHERE store_id = #{storeId}
              AND paid_at >= #{startTime}
              AND paid_at <= #{endTime}
              AND deleted = 0
            """)
    BigDecimal sumPaidAmountByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM refund_record
            WHERE store_id = #{storeId}
              AND refunded_at >= #{startTime}
              AND refunded_at <= #{endTime}
              AND deleted = 0
            """)
    BigDecimal sumRefundAmountByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COALESCE(SUM(official_settlement_amount), 0)
            FROM official_after_sales
            WHERE store_id = #{storeId}
              AND official_settlement_status = 'SETTLED'
              AND official_settlement_time >= #{startTime}
              AND official_settlement_time <= #{endTime}
              AND deleted = 0
            """)
    BigDecimal sumOfficialSettlementByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COALESCE(SUM(woci.line_cost_amount), 0)
            FROM work_order_charge_item woci
            INNER JOIN work_order wo ON wo.id = woci.work_order_id AND wo.deleted = 0
            WHERE woci.store_id = #{storeId}
              AND wo.status = 'SETTLED'
              AND wo.settled_at >= #{startTime}
              AND wo.settled_at <= #{endTime}
              AND woci.deleted = 0
            """)
    BigDecimal sumPartsCostByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COALESCE(SUM(confirmed_amount), 0)
            FROM reimbursement
            WHERE store_id = #{storeId}
              AND status = 'CONFIRMED'
              AND confirmed_at >= #{startTime}
              AND confirmed_at <= #{endTime}
              AND deleted = 0
            """)
    BigDecimal sumConfirmedReimbursementByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(*)
            FROM work_order
            WHERE store_id = #{storeId}
              AND status = 'SETTLED'
              AND settled_at >= #{startTime}
              AND settled_at <= #{endTime}
              AND deleted = 0
            """)
    Integer countSettledWorkOrdersByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(*)
            FROM reimbursement
            WHERE store_id = #{storeId}
              AND status = 'CONFIRMED'
              AND confirmed_at >= #{startTime}
              AND confirmed_at <= #{endTime}
              AND deleted = 0
            """)
    Integer countConfirmedReimbursementsByStoreAndTimeRange(@Param("storeId") Long storeId,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);
}
