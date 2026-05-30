package com.xiaoniu.aftermarket.part.controller;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateOfficialPartRequest(
        @NotBlank(message = "配件名称不能为空") String partName,
        @NotBlank(message = "官方配件必须填写品号") String officialPartNo,
        String model,
        String categoryCode,
        BigDecimal referenceCostPrice,
        BigDecimal defaultSalePrice,
        String defaultBarcode,
        String locationRemark,
        String remark
) {
}
