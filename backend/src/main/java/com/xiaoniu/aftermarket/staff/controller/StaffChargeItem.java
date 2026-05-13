package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import java.math.BigDecimal;

public record StaffChargeItem(
        Long id,
        String chargeType,
        String itemName,
        Long partId,
        String partCodeSnapshot,
        String partNameSnapshot,
        String partSourceSnapshot,
        Integer quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal lineAmount,
        String remark
) {

    public static StaffChargeItem from(WorkOrderChargeItemResponse r) {
        return new StaffChargeItem(
                r.getId(),
                r.getChargeType(),
                r.getItemName(),
                r.getPartId(),
                r.getPartCodeSnapshot(),
                r.getPartNameSnapshot(),
                r.getPartSourceSnapshot(),
                r.getQuantity(),
                r.getUnit(),
                r.getUnitPrice(),
                r.getLineAmount(),
                r.getRemark()
        );
    }
}
