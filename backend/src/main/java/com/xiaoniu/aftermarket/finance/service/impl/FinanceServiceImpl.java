package com.xiaoniu.aftermarket.finance.service.impl;

import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FinanceServiceImpl implements FinanceService {

    @Override
    public Map<String, BigDecimal> queryDaily(LocalDate date) {
        throw new UnsupportedOperationException("TODO: implement finance daily query in Phase 7");
    }

    @Override
    public Map<String, BigDecimal> queryMonthly(int year, int month) {
        throw new UnsupportedOperationException("TODO: implement finance monthly query in Phase 7");
    }

    @Override
    public Map<String, BigDecimal> queryRange(LocalDate startDate, LocalDate endDate) {
        throw new UnsupportedOperationException("TODO: implement finance range query in Phase 7");
    }
}
