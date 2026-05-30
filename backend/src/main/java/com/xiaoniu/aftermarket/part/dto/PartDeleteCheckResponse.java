package com.xiaoniu.aftermarket.part.dto;

import java.util.List;

public record PartDeleteCheckResponse(
        boolean canDelete,
        List<String> reasons,
        StockSummary stockSummary,
        ReferenceSummary referenceSummary
) {

    public record StockSummary(
            int actualQty,
            int availableQty,
            int reservedQty
    ) {
    }

    public record ReferenceSummary(
            long inventoryFlowCount,
            long workOrderChargeItemCount,
            List<Long> sampleWorkOrderIds
    ) {
    }
}
