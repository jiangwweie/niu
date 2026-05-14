package com.xiaoniu.aftermarket.staff.dto;

public record StaffUpdateDraftWorkOrderRequest(
    String customerNameSnapshot,
    String customerPhoneSnapshot,
    String vehicleModelSnapshot,
    String frameNoSnapshot,
    String batteryNoSnapshot,
    String repairItem,
    String remark
) {}
