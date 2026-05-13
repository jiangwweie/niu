package com.xiaoniu.aftermarket.part.controller;

import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import java.math.BigDecimal;

public record PartDetailResponse(
        Long id,
        Long storeId,
        String partCode,
        String officialPartNo,
        String partName,
        String model,
        String source,
        String categoryCode,
        BigDecimal referenceCostPrice,
        String defaultBarcode,
        String locationRemark,
        String createSource,
        String status,
        String remark
) {

    public static PartDetailResponse fromQueryResponse(PartQueryResponse r) {
        return new PartDetailResponse(
                r.getId(),
                r.getStoreId(),
                r.getPartCode(),
                r.getOfficialPartNo(),
                r.getPartName(),
                r.getModel(),
                r.getSource(),
                r.getCategoryCode(),
                r.getReferenceCostPrice(),
                r.getDefaultBarcode(),
                r.getLocationRemark(),
                r.getCreateSource(),
                r.getStatus(),
                r.getRemark()
        );
    }
}
