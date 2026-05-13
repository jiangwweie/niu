package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.part.entity.PartEntity;

public record StaffPartDetail(
        Long id,
        String partCode,
        String officialPartNo,
        String partName,
        String model,
        String source,
        String categoryCode,
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
                entity.getStatus(),
                entity.getRemark()
        );
    }
}
