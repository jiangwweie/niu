package com.xiaoniu.aftermarket.workorder.controller;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemInput;
import java.util.List;

public record CreateDraftRequest(
        Long customerId,
        Long vehicleId,
        String customerNameSnapshot,
        String customerPhoneSnapshot,
        String vehicleModelSnapshot,
        String frameNoSnapshot,
        String batteryNoSnapshot,
        String repairItem,
        String remark,
        List<WorkOrderChargeItemInput> chargeItems
) {
}
