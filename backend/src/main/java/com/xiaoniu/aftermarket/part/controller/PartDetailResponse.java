package com.xiaoniu.aftermarket.part.controller;

import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal defaultSalePrice,
        String defaultBarcode,
        String locationRemark,
        String createSource,
        String status,
        String remark,
        Boolean canDelete,
        List<String> deleteReasons,
        String deleteBlockReasonSummary,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty,
        Long inventoryFlowCount,
        Long workOrderChargeItemCount,
        Boolean archived,
        Boolean hasHistoryReference
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
                r.getDefaultSalePrice(),
                r.getDefaultBarcode(),
                r.getLocationRemark(),
                r.getCreateSource(),
                r.getStatus(),
                r.getRemark(),
                r.getCanDelete(),
                r.getDeleteReasons(),
                r.getDeleteBlockReasonSummary(),
                r.getActualQty(),
                r.getAvailableQty(),
                r.getReservedQty(),
                r.getInventoryFlowCount(),
                r.getWorkOrderChargeItemCount(),
                r.getArchived(),
                r.getHasHistoryReference()
        );
    }
}
