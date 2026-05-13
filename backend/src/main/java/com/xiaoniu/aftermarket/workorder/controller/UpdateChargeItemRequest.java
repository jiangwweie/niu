package com.xiaoniu.aftermarket.workorder.controller;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateChargeItemRequest(
        String itemName,
        @NotNull(message = "数量不能为空") Integer quantity,
        String unit,
        BigDecimal unitPrice,
        String remark
) {
}
