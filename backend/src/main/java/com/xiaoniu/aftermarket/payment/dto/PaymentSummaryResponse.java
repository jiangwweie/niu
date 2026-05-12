package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentSummaryResponse {

    private Long workOrderId;
    private BigDecimal receivableAmount;
    private BigDecimal paymentTotal;
    private BigDecimal refundTotal;
    private BigDecimal receivedAmount;
    private Boolean canSettle;
}
