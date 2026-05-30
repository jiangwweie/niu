package com.xiaoniu.aftermarket.part.dto;

import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.math.BigDecimal;

public record PartLookupResponse(
        boolean matched,
        Long partId,
        String partCode,
        String partName,
        String officialPartNo,
        String defaultBarcode,
        String source,
        String name,
        String model,
        String categoryCode,
        String category,
        BigDecimal costPrice,
        BigDecimal salePrice,
        Boolean enabled,
        Boolean hasStockRecord,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty
) {

    public static PartLookupResponse notMatched() {
        return new PartLookupResponse(false, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static PartLookupResponse matched(PartEntity part, InventoryStockEntity stock) {
        return new PartLookupResponse(
                true,
                part.getId(),
                part.getPartCode(),
                part.getPartName(),
                part.getOfficialPartNo(),
                part.getDefaultBarcode(),
                part.getSource(),
                part.getPartName(),
                part.getModel(),
                part.getCategoryCode(),
                part.getCategoryCode(),
                part.getReferenceCostPrice(),
                part.getDefaultSalePrice(),
                "ENABLED".equals(part.getStatus()),
                stock != null,
                stock != null ? stock.getActualQty() : 0,
                stock != null ? stock.getAvailableQty() : 0,
                stock != null ? stock.getReservedQty() : 0
        );
    }
}
