package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StaffWorkOrderDetail(
        Long id,
        Long customerId,
        Long vehicleId,
        String workOrderNo,
        String customerNameSnapshot,
        String customerPhoneSnapshot,
        String vehicleModelSnapshot,
        String frameNoSnapshot,
        String batteryNoSnapshot,
        String repairItem,
        String status,
        String progressStatus,
        String progressStatusText,
        String cashierStatus,
        String cashierStatusText,
        String inventoryStatus,
        String inventoryStatusText,
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        BigDecimal paymentTotal,
        BigDecimal refundTotal,
        BigDecimal netReceived,
        BigDecimal outstandingAmount,
        BigDecimal refundableAmount,
        String noChargeReason,
        String noChargeRemark,
        Boolean canMarkRepairDone,
        Boolean canDeliver,
        Boolean canCancel,
        Boolean canRecordPayment,
        Boolean canRecordRefund,
        Boolean canRefundAfterDelivery,
        String remark,
        LocalDateTime createdAt,
        List<StaffChargeItem> chargeItems
) {

    public static StaffWorkOrderDetail from(WorkOrderDetailResponse d) {
        List<StaffChargeItem> items = d.getChargeItems() != null
                ? d.getChargeItems().stream().map(StaffChargeItem::from).toList()
                : List.of();
        return new StaffWorkOrderDetail(
                d.getId(),
                d.getCustomerId(),
                d.getVehicleId(),
                d.getWorkOrderNo(),
                d.getCustomerNameSnapshot(),
                d.getCustomerPhoneSnapshot(),
                d.getVehicleModelSnapshot(),
                d.getFrameNoSnapshot(),
                d.getBatteryNoSnapshot(),
                d.getRepairItem(),
                d.getStatus(),
                d.getProgressStatus(),
                d.getProgressStatusText(),
                d.getCashierStatus(),
                d.getCashierStatusText(),
                d.getInventoryStatus(),
                d.getInventoryStatusText(),
                d.getReceivableAmount(),
                d.getReceivedAmount(),
                d.getPaymentTotal(),
                d.getRefundTotal(),
                d.getNetReceived(),
                d.getOutstandingAmount(),
                d.getRefundableAmount(),
                d.getNoChargeReason(),
                d.getNoChargeRemark(),
                d.getCanMarkRepairDone(),
                d.getCanDeliver(),
                d.getCanCancel(),
                d.getCanRecordPayment(),
                d.getCanRecordRefund(),
                d.getCanRefundAfterDelivery(),
                d.getRemark(),
                d.getCreatedAt(),
                items
        );
    }
}
