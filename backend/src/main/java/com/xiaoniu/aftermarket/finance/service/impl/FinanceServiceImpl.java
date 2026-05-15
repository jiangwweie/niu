package com.xiaoniu.aftermarket.finance.service.impl;

import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.mapper.FinanceMapper;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

@Service
public class FinanceServiceImpl implements FinanceService {

    private final FinanceMapper financeMapper;

    public FinanceServiceImpl(FinanceMapper financeMapper) {
        this.financeMapper = financeMapper;
    }

    @Override
    public FinanceReportResponse queryDaily(Long storeId, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);
        return buildReport(storeId, date, date, startTime, endTime);
    }

    @Override
    public FinanceReportResponse queryMonthly(Long storeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return buildReport(storeId, startDate, endDate, startTime, endTime);
    }

    @Override
    public FinanceReportResponse queryRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return buildReport(storeId, startDate, endDate, startTime, endTime);
    }

    private FinanceReportResponse buildReport(Long storeId, LocalDate periodStart, LocalDate periodEnd,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        BigDecimal totalPaid = financeMapper.sumPaidAmountByStoreAndTimeRange(storeId, startTime, endTime);
        BigDecimal totalRefund = financeMapper.sumRefundAmountByStoreAndTimeRange(storeId, startTime, endTime);
        BigDecimal customerIncome = totalPaid.subtract(totalRefund).setScale(2, RoundingMode.HALF_UP);

        BigDecimal officialIncome = normalize(financeMapper.sumOfficialSettlementByStoreAndTimeRange(storeId, startTime, endTime));
        BigDecimal partsCost = normalize(financeMapper.sumPartsCostByStoreAndTimeRange(storeId, startTime, endTime));
        BigDecimal reimbursementCost = normalize(financeMapper.sumConfirmedReimbursementByStoreAndTimeRange(storeId, startTime, endTime));

        BigDecimal totalIncome = customerIncome.add(officialIncome).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = partsCost.add(reimbursementCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profit = totalIncome.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

        Integer settledWorkOrderCount = financeMapper.countSettledWorkOrdersByStoreAndTimeRange(storeId, startTime, endTime);
        Integer confirmedReimbursementCount = financeMapper.countConfirmedReimbursementsByStoreAndTimeRange(storeId, startTime, endTime);

        FinanceReportResponse response = new FinanceReportResponse();
        response.setStoreId(storeId);
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setCustomerIncome(customerIncome);
        response.setOfficialIncome(officialIncome);
        response.setPartsCost(partsCost);
        response.setReimbursementCost(reimbursementCost);
        response.setTotalIncome(totalIncome);
        response.setTotalCost(totalCost);
        response.setProfit(profit);
        response.setSettledWorkOrderCount(settledWorkOrderCount);
        response.setConfirmedReimbursementCount(confirmedReimbursementCount);
        return response;
    }

    private BigDecimal normalize(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
