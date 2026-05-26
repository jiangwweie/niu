package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffTempPartChargeRequest(
        String source,
        @NotBlank(message = "partName不能为空") String partName,
        String officialPartNo,
        String barcode,
        String model,
        String categoryCode,
        @NotNull(message = "quantity不能为空") @Positive(message = "quantity必须大于0") Integer quantity,
        String unit,
        @NotNull(message = "unitPrice不能为空") @DecimalMin(value = "0", message = "unitPrice不能为负数") BigDecimal unitPrice,
        @DecimalMin(value = "0", message = "unitCost不能为负数") BigDecimal unitCost,
        String locationRemark,
        String remark
) {
}
