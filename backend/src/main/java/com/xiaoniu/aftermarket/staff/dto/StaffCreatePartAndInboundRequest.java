package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StaffCreatePartAndInboundRequest(
        @NotBlank(message = "source不能为空") String source,
        @NotBlank(message = "partName不能为空") String partName,
        String officialPartNo,
        String externalBarcode,
        String model,
        String categoryCode,
        BigDecimal costPrice,
        BigDecimal salePrice,
        @NotNull(message = "inboundQuantity不能为空") @Positive(message = "inboundQuantity必须大于0") Integer inboundQuantity,
        @DecimalMin(value = "0", message = "unitCost不能为负数") BigDecimal unitCost,
        String locationRemark,
        String reason,
        String remark
) {
}

