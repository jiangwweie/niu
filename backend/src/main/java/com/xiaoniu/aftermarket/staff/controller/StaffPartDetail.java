package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.math.BigDecimal;

public record StaffPartDetail(
        Long id,
        String partCode,
        String officialPartNo,
        String partName,
        String model,
        String source,
        String categoryCode,
        BigDecimal costPrice,
        BigDecimal salePrice,
        String status,
        String remark
) {

    public static StaffPartDetail from(PartEntity entity) {
        return new StaffPartDetail(
                entity.getId(),
                entity.getPartCode(),
                entity.getOfficialPartNo(),
                entity.getPartName(),
                entity.getModel(),
                entity.getSource(),
                entity.getCategoryCode(),
                entity.getReferenceCostPrice(),
                entity.getDefaultSalePrice(),
                entity.getStatus(),
                entity.getRemark()
        );
    }
}
