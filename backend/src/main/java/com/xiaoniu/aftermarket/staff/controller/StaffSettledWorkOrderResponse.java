package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StaffSettledWorkOrderResponse(
        Long workOrderId,
        String workOrderNo,
        String status,
        BigDecimal receivableAmount,
        BigDecimal receivedAmount,
        LocalDateTime settledAt,
        Long settlerId,
        List<StaffChargeItem> chargeItems
) {

    public static StaffSettledWorkOrderResponse from(WorkOrderDetailResponse detail, WorkOrderEntity entity) {
        List<StaffChargeItem> items = detail.getChargeItems() != null
                ? detail.getChargeItems().stream().map(StaffChargeItem::from).toList()
                : List.of();
        return new StaffSettledWorkOrderResponse(
                detail.getId(),
                detail.getWorkOrderNo(),
                detail.getStatus(),
                detail.getReceivableAmount(),
                detail.getReceivedAmount(),
                entity.getSettledAt(),
                entity.getSettledBy(),
                items
        );
    }
}
