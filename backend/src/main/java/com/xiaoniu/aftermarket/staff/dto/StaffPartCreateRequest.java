package com.xiaoniu.aftermarket.staff.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record StaffPartCreateRequest(
        String source,
        @NotBlank(message = "partName不能为空") String partName,
        String officialPartNo,
        String externalBarcode,
        String model,
        String categoryCode,
        BigDecimal costPrice,
        BigDecimal salePrice,
        String locationRemark,
        String remark
) {
}

