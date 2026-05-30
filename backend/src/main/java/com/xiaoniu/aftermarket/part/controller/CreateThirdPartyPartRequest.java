package com.xiaoniu.aftermarket.part.controller;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateThirdPartyPartRequest(
        @NotBlank(message = "配件名称不能为空") String partName,
        String model,
        String categoryCode,
        BigDecimal referenceCostPrice,
        BigDecimal defaultSalePrice,
        String defaultBarcode,
        String locationRemark,
        String remark
) {
}
