package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentRecordResponse {

    private Long id;
    private Long workOrderId;
    private String paymentNo;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Long receiverId;
    private String receiverName;
    private Long operatorId;
    private String operatorName;
    private String remark;
}
