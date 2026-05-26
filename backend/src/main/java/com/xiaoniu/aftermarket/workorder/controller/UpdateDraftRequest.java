package com.xiaoniu.aftermarket.workorder.controller;

public record UpdateDraftRequest(
        Long customerId,
        Long vehicleId,
        String customerNameSnapshot,
        String customerPhoneSnapshot,
        String vehicleModelSnapshot,
        String frameNoSnapshot,
        String batteryNoSnapshot,
        String repairItem,
        String remark
) {
}
