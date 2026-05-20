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
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
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
                d.getReceivableAmount(),
                d.getReceivedAmount(),
                d.getRemark(),
                d.getCreatedAt(),
                items
        );
    }
}
