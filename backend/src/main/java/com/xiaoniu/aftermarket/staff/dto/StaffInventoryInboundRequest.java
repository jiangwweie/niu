package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffInventoryInboundRequest(
    Long partId,
    @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
    @DecimalMin(value = "0", message = "unitCost不能为负数") BigDecimal unitCost,
    String barcode,
    String code,
    String locationRemark,
    String reason,
    String remark
) {}
