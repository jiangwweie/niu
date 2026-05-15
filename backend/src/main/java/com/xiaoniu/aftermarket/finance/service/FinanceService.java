package com.xiaoniu.aftermarket.finance.service;

import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import java.time.LocalDate;

public interface FinanceService {

    FinanceReportResponse queryDaily(Long storeId, LocalDate date);

    FinanceReportResponse queryMonthly(Long storeId, int year, int month);

    FinanceReportResponse queryRange(Long storeId, LocalDate startDate, LocalDate endDate);
}
