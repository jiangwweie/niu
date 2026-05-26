package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffAddChargeItemRequest(
    @NotBlank(message = "chargeType不能为空") String chargeType,
    @NotBlank(message = "itemName不能为空") String itemName,
    Long partId,
    String barcode,
    String code,
    @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
    String unit,
    @NotNull(message = "unitPrice不能为空") BigDecimal unitPrice,
    String remark
) {}
