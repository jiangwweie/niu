package com.xiaoniu.aftermarket.payment.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RecordRefundRequest {

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "退款方式不能为空")
    private String refundMethod;

    private LocalDateTime refundedAt;

    @NotBlank(message = "退款原因不能为空")
    private String reason;

    private String remark;
}
