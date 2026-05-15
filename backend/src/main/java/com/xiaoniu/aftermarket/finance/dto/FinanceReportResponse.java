package com.xiaoniu.aftermarket.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinanceReportResponse {

    private Long storeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private BigDecimal customerIncome;
    private BigDecimal officialIncome;
    private BigDecimal partsCost;
    private BigDecimal reimbursementCost;

    private BigDecimal totalIncome;
    private BigDecimal totalCost;
    private BigDecimal profit;

    private Integer settledWorkOrderCount;
    private Integer confirmedReimbursementCount;
}
