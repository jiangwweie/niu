package com.xiaoniu.aftermarket.staff.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffCreatePartAndInboundResponse(
        Long partId,
        String partCode,
        String partName,
        String defaultBarcode,
        String externalBarcode,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty,
        Long flowId,
        LocalDateTime operatedAt
) {
}

