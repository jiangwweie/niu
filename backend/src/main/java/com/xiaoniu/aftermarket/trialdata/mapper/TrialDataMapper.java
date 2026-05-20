package com.xiaoniu.aftermarket.trialdata.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrialDataMapper {

    @Select("SELECT COUNT(*) FROM work_order WHERE store_id = #{storeId}")
    Integer countWorkOrders(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM work_order_charge_item WHERE store_id = #{storeId}")
    Integer countWorkOrderChargeItems(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM work_order_status_log WHERE store_id = #{storeId}")
    Integer countWorkOrderStatusLogs(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM payment_record WHERE store_id = #{storeId}")
    Integer countPaymentRecords(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM refund_record WHERE store_id = #{storeId}")
    Integer countRefundRecords(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM official_after_sales WHERE store_id = #{storeId}")
    Integer countOfficialAfterSales(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM reimbursement WHERE store_id = #{storeId}")
    Integer countReimbursements(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM inventory_flow WHERE store_id = #{storeId}")
    Integer countInventoryFlows(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM inventory_stock WHERE store_id = #{storeId}")
    Integer countInventoryStocks(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM vehicle WHERE store_id = #{storeId}")
    Integer countVehicles(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM customer WHERE store_id = #{storeId}")
    Integer countCustomers(@Param("storeId") Long storeId);

    // --- Delete operations ---

    @Update("DELETE FROM work_order_status_log WHERE store_id = #{storeId}")
    int deleteWorkOrderStatusLogs(@Param("storeId") Long storeId);

    @Update("DELETE FROM work_order_charge_item WHERE store_id = #{storeId}")
    int deleteWorkOrderChargeItems(@Param("storeId") Long storeId);

    @Update("DELETE FROM payment_record WHERE store_id = #{storeId}")
    int deletePaymentRecords(@Param("storeId") Long storeId);

    @Update("DELETE FROM refund_record WHERE store_id = #{storeId}")
    int deleteRefundRecords(@Param("storeId") Long storeId);

    @Update("DELETE FROM official_after_sales WHERE store_id = #{storeId}")
    int deleteOfficialAfterSales(@Param("storeId") Long storeId);

    @Update("DELETE FROM reimbursement WHERE store_id = #{storeId}")
    int deleteReimbursements(@Param("storeId") Long storeId);

    @Update("DELETE FROM work_order WHERE store_id = #{storeId}")
    int deleteWorkOrders(@Param("storeId") Long storeId);

    @Update("DELETE FROM inventory_flow WHERE store_id = #{storeId}")
    int deleteInventoryFlows(@Param("storeId") Long storeId);

    @Update("DELETE FROM vehicle WHERE store_id = #{storeId}")
    int deleteVehicles(@Param("storeId") Long storeId);

    @Update("DELETE FROM customer WHERE store_id = #{storeId}")
    int deleteCustomers(@Param("storeId") Long storeId);

    @Update("""
            UPDATE inventory_stock
            SET actual_qty = 0, available_qty = 0, reserved_qty = 0,
                last_flow_id = NULL, last_changed_at = NULL
            WHERE store_id = #{storeId}
            """)
    int resetInventoryStocks(@Param("storeId") Long storeId);
}
