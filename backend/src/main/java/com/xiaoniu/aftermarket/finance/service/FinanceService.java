package com.xiaoniu.aftermarket.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface FinanceService {

    Map<String, BigDecimal> queryDaily(LocalDate date);

    Map<String, BigDecimal> queryMonthly(int year, int month);

    Map<String, BigDecimal> queryRange(LocalDate startDate, LocalDate endDate);
}
