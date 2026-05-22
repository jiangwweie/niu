package com.xiaoniu.aftermarket.staff.dto;

public record StaffDeliverWorkOrderRequest(
        String noChargeReason,
        String noChargeRemark,
        String remark
) {}
