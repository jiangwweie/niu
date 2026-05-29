package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RefundRecordResponse {

    private Long id;
    private Long workOrderId;
    private String refundNo;
    private BigDecimal amount;
    private String refundMethod;
    private LocalDateTime refundedAt;
    private Long operatorId;
    private String operatorName;
    private String reason;
    private String remark;
}
