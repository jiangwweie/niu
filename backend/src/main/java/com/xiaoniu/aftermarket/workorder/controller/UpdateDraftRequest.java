package com.xiaoniu.aftermarket.workorder.controller;

public record UpdateDraftRequest(
        String customerNameSnapshot,
        String customerPhoneSnapshot,
        String vehicleModelSnapshot,
        String frameNoSnapshot,
        String batteryNoSnapshot,
        String repairItem,
        String remark
) {
}
