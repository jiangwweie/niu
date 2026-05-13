package com.xiaoniu.aftermarket.workorder.controller;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AddChargeItemRequest(
        @NotBlank(message = "收费类型不能为空") String chargeType,
        String itemName,
        Long partId,
        Integer quantity,
        String unit,
        BigDecimal unitPrice,
        String remark
) {
}
