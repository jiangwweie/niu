package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffInventoryInboundRequest(
    @NotNull(message = "partId不能为空") Long partId,
    @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
    @DecimalMin(value = "0", message = "unitCost不能为负数") BigDecimal unitCost,
    String barcode,
    String locationRemark,
    String reason,
    String remark
) {}
