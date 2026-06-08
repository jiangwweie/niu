package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import java.math.BigDecimal;

public record StaffPartListItem(
        Long id,
        String partCode,
        String officialPartNo,
        String partName,
        String model,
        String source,
        String categoryCode,
        BigDecimal costPrice,
        BigDecimal salePrice,
        String locationRemark,
        String status
) {

    public static StaffPartListItem from(PartQueryResponse r) {
        return new StaffPartListItem(
                r.getId(),
                r.getPartCode(),
                r.getOfficialPartNo(),
                r.getPartName(),
                r.getModel(),
                r.getSource(),
                r.getCategoryCode(),
                r.getReferenceCostPrice(),
                r.getDefaultSalePrice(),
                r.getLocationRemark(),
                r.getStatus()
        );
    }
}
