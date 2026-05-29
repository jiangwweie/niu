package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffRefundRecordResponse(
        Long id,
        Long workOrderId,
        String refundNo,
        BigDecimal amount,
        String refundMethod,
        LocalDateTime refundedAt,
        Long operatorId,
        String operatorName,
        String reason,
        String remark
) {

    public static StaffRefundRecordResponse from(RefundRecordResponse response) {
        return new StaffRefundRecordResponse(
                response.getId(),
                response.getWorkOrderId(),
                response.getRefundNo(),
                response.getAmount(),
                response.getRefundMethod(),
                response.getRefundedAt(),
                response.getOperatorId(),
                response.getOperatorName(),
                response.getReason(),
                response.getRemark()
        );
    }
}
