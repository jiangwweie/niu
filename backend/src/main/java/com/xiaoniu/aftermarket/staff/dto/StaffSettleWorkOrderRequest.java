package com.xiaoniu.aftermarket.staff.dto;

import java.time.LocalDateTime;

public record StaffSettleWorkOrderRequest(
        LocalDateTime settledAt,
        String remark
) {
}
