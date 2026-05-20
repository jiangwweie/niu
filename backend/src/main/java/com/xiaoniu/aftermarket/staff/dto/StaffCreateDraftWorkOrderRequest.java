package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffCreateDraftWorkOrderRequest(
    Long customerId,
    Long vehicleId,
    @NotBlank(message = "customerNameSnapshot不能为空") String customerNameSnapshot,
    String customerPhoneSnapshot,
    String vehicleModelSnapshot,
    String frameNoSnapshot,
    String batteryNoSnapshot,
    @NotBlank(message = "repairItem不能为空") String repairItem,
    String remark
) {}
