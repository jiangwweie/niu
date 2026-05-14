package com.xiaoniu.aftermarket.reimbursement.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConfirmReimbursementRequest(
        @NotNull(message = "确认金额不能为空")
        @DecimalMin(value = "0.01", message = "确认金额必须大于0")
        BigDecimal confirmedAmount,

        String remark
) {
}
