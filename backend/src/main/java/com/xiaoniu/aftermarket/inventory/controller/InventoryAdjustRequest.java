package com.xiaoniu.aftermarket.inventory.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustRequest(
        @NotNull(message = "配件ID不能为空") Long partId,
        @NotNull(message = "调整数量不能为空") Integer quantityDelta,
        @NotBlank(message = "调整原因不能为空") String reason,
        String remark
) {
}
