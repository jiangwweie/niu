package com.xiaoniu.aftermarket.inventory.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InventoryInboundRequest(
        @NotNull(message = "配件ID不能为空") Long partId,
        @NotNull(message = "入库数量不能为空") @Positive(message = "入库数量必须大于0") Integer quantity,
        BigDecimal unitCost,
        String barcode,
        String locationRemark,
        String reason,
        String remark
) {
}
