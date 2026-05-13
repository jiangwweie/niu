package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentQueryResponse {

    private Long id;
    private Long workOrderId;
    private String workOrderNo;
    private String customerNameSnapshot;
    private String paymentNo;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Long receiverId;
    private Long operatorId;
    private String remark;
}
