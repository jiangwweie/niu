package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffWorkOrderListItem(
        Long id,
        String workOrderNo,
        String customerNameSnapshot,
        String customerPhoneSnapshot,
        String vehicleModelSnapshot,
        String frameNoSnapshot,
        String status,
        String progressStatus,
        String progressStatusText,
        String cashierStatus,
        String cashierStatusText,
        String inventoryStatus,
        String inventoryStatusText,
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        BigDecimal outstandingAmount,
        LocalDateTime createdAt
) {

    public static StaffWorkOrderListItem from(WorkOrderQueryResponse r) {
        return new StaffWorkOrderListItem(
                r.getId(),
                r.getWorkOrderNo(),
                r.getCustomerNameSnapshot(),
                r.getCustomerPhoneSnapshot(),
                r.getVehicleModelSnapshot(),
                r.getFrameNoSnapshot(),
                r.getStatus(),
                r.getProgressStatus(),
                r.getProgressStatusText(),
                r.getCashierStatus(),
                r.getCashierStatusText(),
                r.getInventoryStatus(),
                r.getInventoryStatusText(),
                r.getReceivableAmount(),
                r.getReceivedAmount(),
                r.getOutstandingAmount(),
                r.getCreatedAt()
        );
    }
}
