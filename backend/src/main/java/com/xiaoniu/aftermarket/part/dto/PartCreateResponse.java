package com.xiaoniu.aftermarket.part.dto;

import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.math.BigDecimal;

public record PartCreateResponse(
        Long partId,
        String partCode,
        String defaultBarcode,
        String officialPartNo,
        String source,
        String name,
        String model,
        String categoryCode,
        BigDecimal costPrice,
        BigDecimal salePrice,
        Boolean enabled
) {

    public static PartCreateResponse from(PartEntity part) {
        return new PartCreateResponse(
                part.getId(),
                part.getPartCode(),
                part.getDefaultBarcode(),
                part.getOfficialPartNo(),
                part.getSource(),
                part.getPartName(),
                part.getModel(),
                part.getCategoryCode(),
                part.getReferenceCostPrice(),
                part.getDefaultSalePrice(),
                "ENABLED".equals(part.getStatus())
        );
    }
}

