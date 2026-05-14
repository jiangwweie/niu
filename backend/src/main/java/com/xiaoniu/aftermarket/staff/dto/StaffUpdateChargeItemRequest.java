package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffUpdateChargeItemRequest(
    String itemName,
    @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
    String unit,
    BigDecimal unitPrice,
    String remark
) {}