package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffPaymentRecordResponse(
        Long id,
        Long workOrderId,
        String paymentNo,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime paidAt,
        Long receiverId,
        String receiverName,
        Long operatorId,
        String operatorName,
        String remark
) {

    public static StaffPaymentRecordResponse from(PaymentRecordResponse response) {
        return new StaffPaymentRecordResponse(
                response.getId(),
                response.getWorkOrderId(),
                response.getPaymentNo(),
                response.getAmount(),
                response.getPaymentMethod(),
                response.getPaidAt(),
                response.getReceiverId(),
                response.getReceiverName(),
                response.getOperatorId(),
                response.getOperatorName(),
                response.getRemark()
        );
    }
}
