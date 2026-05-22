package com.xiaoniu.aftermarket.workorder.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AddNonInventoryChargeRequest(
        @NotBlank(message = "chargeType不能为空") String chargeType,
        @NotBlank(message = "itemName不能为空") String itemName,
        @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
        String unit,
        @NotNull(message = "unitPrice不能为空") BigDecimal unitPrice,
        @NotBlank(message = "reason不能为空") String reason,
        String remark
) {}
