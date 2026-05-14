package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffRecordPaymentRequest(
        @NotNull(message = "支付金额不能为空")
        @DecimalMin(value = "0.01", message = "支付金额必须大于0")
        BigDecimal amount,

        @NotBlank(message = "支付方式不能为空")
        String paymentMethod,

        LocalDateTime paidAt,
        Long receiverId,
        String remark
) {
}
