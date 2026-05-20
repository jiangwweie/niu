package com.xiaoniu.aftermarket.dashboard.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT COUNT(*) FROM work_order
            WHERE store_id = #{storeId}
              AND DATE(created_at) = DATE(#{now})
              AND deleted = 0
            """)
    Integer countTodayWorkOrders(@Param("storeId") Long storeId, @Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(*) FROM work_order
            WHERE store_id = #{storeId}
              AND status NOT IN ('SETTLED', 'CANCELLED')
              AND deleted = 0
            """)
    Integer countPendingSettleWorkOrders(@Param("storeId") Long storeId);

    @Select("""
            SELECT COUNT(*) FROM reimbursement
            WHERE store_id = #{storeId}
              AND status = 'PENDING'
              AND deleted = 0
            """)
    Integer countPendingReimbursements(@Param("storeId") Long storeId);

    @Select("""
            SELECT COUNT(*) FROM inventory_stock ist
            INNER JOIN part p ON p.id = ist.part_id AND p.deleted = 0 AND p.status = 'ENABLED'
            WHERE ist.store_id = #{storeId}
              AND ist.available_qty <= 3
            """)
    Integer countLowStockParts(@Param("storeId") Long storeId);

    @Select("""
            SELECT COUNT(*) FROM official_after_sales
            WHERE store_id = #{storeId}
              AND official_settlement_status = 'PENDING'
              AND deleted = 0
            """)
    Integer countPendingOfficialSettlements(@Param("storeId") Long storeId);

    @Select("""
            SELECT
                wo.id,
                wo.work_order_no,
                wo.customer_name_snapshot,
                wo.status,
                wo.receivable_amount,
                wo.received_amount,
                wo.created_at
            FROM work_order wo
            WHERE wo.store_id = #{storeId}
              AND wo.deleted = 0
            ORDER BY wo.created_at DESC
            FETCH FIRST 5 ROWS ONLY
            """)
    @Results(id = "recentWorkOrderResults", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "workOrderNo", column = "work_order_no"),
            @Result(property = "customerName", column = "customer_name_snapshot"),
            @Result(property = "status", column = "status"),
            @Result(property = "receivableAmount", column = "receivable_amount"),
            @Result(property = "receivedAmount", column = "received_amount"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<RecentWorkOrderRow> findRecentWorkOrders(@Param("storeId") Long storeId);
}
