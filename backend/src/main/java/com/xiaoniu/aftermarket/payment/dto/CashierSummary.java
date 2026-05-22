package com.xiaoniu.aftermarket.payment.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CashierSummary {

    private BigDecimal receivableAmount;
    private BigDecimal paymentTotal;
    private BigDecimal refundTotal;
    private BigDecimal netReceived;
    private BigDecimal outstandingAmount;
    private BigDecimal refundableAmount;
    private String cashierStatus;
    private String cashierStatusText;
}
