package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RecordPaymentCommand {

    private Long storeId;
    private Long workOrderId;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Long receiverId;
    private Long operatorId;
    private String remark;
}
