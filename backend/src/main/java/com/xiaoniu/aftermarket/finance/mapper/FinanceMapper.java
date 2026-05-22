package com.xiaoniu.aftermarket.finance.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinanceMapper {

    record MethodAmount(String method, BigDecimal amount, int count) {}

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
              AND wo.status IN ('REPAIR_DONE', 'DELIVERED')
              AND COALESCE(wo.repair_done_at, wo.delivered_at, wo.settled_at) >= #{startTime}
              AND COALESCE(wo.repair_done_at, wo.delivered_at, wo.settled_at) <= #{endTime}
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
              AND status = 'DELIVERED'
              AND COALESCE(delivered_at, settled_at) >= #{startTime}
              AND COALESCE(delivered_at, settled_at) <= #{endTime}
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

    // ── 收银日报查询：统计手动录入的收款/退款记录 ──────────────────────────

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_record
            WHERE store_id = #{storeId}
              AND paid_at >= #{startTime}
              AND paid_at < #{endExclusive}
              AND deleted = 0
            """)
    BigDecimal sumPaidByStoreAndDate(@Param("storeId") Long storeId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM refund_record
            WHERE store_id = #{storeId}
              AND refunded_at >= #{startTime}
              AND refunded_at < #{endExclusive}
              AND deleted = 0
            """)
    BigDecimal sumRefundByStoreAndDate(@Param("storeId") Long storeId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT COUNT(*)
            FROM payment_record
            WHERE store_id = #{storeId}
              AND paid_at >= #{startTime}
              AND paid_at < #{endExclusive}
              AND deleted = 0
            """)
    int countPaymentsByStoreAndDate(@Param("storeId") Long storeId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT COUNT(*)
            FROM refund_record
            WHERE store_id = #{storeId}
              AND refunded_at >= #{startTime}
              AND refunded_at < #{endExclusive}
              AND deleted = 0
            """)
    int countRefundsByStoreAndDate(@Param("storeId") Long storeId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT payment_method AS method, COALESCE(SUM(amount), 0) AS amount, COUNT(*) AS count
            FROM payment_record
            WHERE store_id = #{storeId}
              AND paid_at >= #{startTime}
              AND paid_at < #{endExclusive}
              AND deleted = 0
            GROUP BY payment_method
            """)
    List<MethodAmount> sumPaidByStoreDateGroupByMethod(@Param("storeId") Long storeId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT refund_method AS method, COALESCE(SUM(amount), 0) AS amount, COUNT(*) AS count
            FROM refund_record
            WHERE store_id = #{storeId}
              AND refunded_at >= #{startTime}
              AND refunded_at < #{endExclusive}
              AND deleted = 0
            GROUP BY refund_method
            """)
    List<MethodAmount> sumRefundByStoreDateGroupByMethod(@Param("storeId") Long storeId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endExclusive") LocalDateTime endExclusive);

    // 未结清工单：实时快照（当前所有未收齐的工单），不按日期筛选
    @Select("""
            SELECT COUNT(*)
            FROM work_order
            WHERE store_id = #{storeId}
              AND status IN ('REPAIRING', 'REPAIR_DONE')
              AND received_amount < receivable_amount
              AND deleted = 0
            """)
    int countCurrentUnpaidWorkOrders(@Param("storeId") Long storeId);

    // 部分收款工单：实收 > 0 但 < 应收，同样为实时口径
    @Select("""
            SELECT COUNT(*)
            FROM work_order
            WHERE store_id = #{storeId}
              AND status IN ('REPAIRING', 'REPAIR_DONE')
              AND received_amount > 0
              AND received_amount < receivable_amount
              AND deleted = 0
            """)
    int countCurrentPartialPaidWorkOrders(@Param("storeId") Long storeId);
}
