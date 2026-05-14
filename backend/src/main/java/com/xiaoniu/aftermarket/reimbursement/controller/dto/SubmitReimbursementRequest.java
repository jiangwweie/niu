package com.xiaoniu.aftermarket.reimbursement.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubmitReimbursementRequest(
        @NotBlank(message = "报销用途不能为空")
        String purpose,

        @NotNull(message = "报销金额不能为空")
        @DecimalMin(value = "0.01", message = "报销金额必须大于0")
        BigDecimal amount,

        String remark
) {
}
