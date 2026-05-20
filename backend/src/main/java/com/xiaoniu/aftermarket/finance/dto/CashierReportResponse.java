package com.xiaoniu.aftermarket.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashierReportResponse {

    private LocalDate date;
    private Long storeId;
    private String storeName;

    private BigDecimal totalPaymentAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netAmount;
    private int paymentCount;
    private int refundCount;

    private List<MethodBreakdown> byMethod;

    private int currentUnpaidWorkOrderCount;
    private int currentPartialPaidWorkOrderCount;

    @Getter
    @Setter
    public static class MethodBreakdown {
        private String method;
        private BigDecimal paymentAmount;
        private BigDecimal refundAmount;
        private BigDecimal netAmount;
        private int paymentCount;
        private int refundCount;
    }
}
