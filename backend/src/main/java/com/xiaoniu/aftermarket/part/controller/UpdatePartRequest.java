package com.xiaoniu.aftermarket.part.controller;

import java.math.BigDecimal;

public record UpdatePartRequest(
        String partName,
        String officialPartNo,
        String model,
        String categoryCode,
        BigDecimal referenceCostPrice,
        String defaultBarcode,
        String locationRemark,
        String remark
) {
}
