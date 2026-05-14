package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffRecordRefundRequest(
        @NotNull(message = "退款金额不能为空")
        @DecimalMin(value = "0.01", message = "退款金额必须大于0")
        BigDecimal amount,

        @NotBlank(message = "退款方式不能为空")
        String refundMethod,

        LocalDateTime refundedAt,

        @NotBlank(message = "退款原因不能为空")
        String reason,

        String remark
) {
}
