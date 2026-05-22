package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RecordRefundCommand {

    private Long storeId;
    private Long workOrderId;
    private BigDecimal amount;
    private String refundMethod;
    private LocalDateTime refundedAt;
    private Long operatorId;
    private boolean allowDeliveredAfterRefund;
    private String reason;
    private String remark;
}
