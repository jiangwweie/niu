package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffCancelWorkOrderRequest(
        @NotBlank(message = "取消原因不能为空") String reason,
        String remark
) {
}
