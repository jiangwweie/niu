package com.xiaoniu.aftermarket.finance.service.impl;

import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.finance.dto.CashierReportResponse;
import com.xiaoniu.aftermarket.finance.dto.CashierReportResponse.MethodBreakdown;
import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.mapper.FinanceMapper;
import com.xiaoniu.aftermarket.finance.mapper.FinanceMapper.MethodAmount;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Override
    public CashierReportResponse getCashierReport(Long storeId, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endExclusive = date.plusDays(1).atStartOfDay();

        BigDecimal totalPaymentAmount = nz(financeMapper.sumPaidByStoreAndDate(storeId, startTime, endExclusive));
        BigDecimal totalRefundAmount = nz(financeMapper.sumRefundByStoreAndDate(storeId, startTime, endExclusive));
        BigDecimal netAmount = totalPaymentAmount.subtract(totalRefundAmount).setScale(2, RoundingMode.HALF_UP);

        int paymentCount = financeMapper.countPaymentsByStoreAndDate(storeId, startTime, endExclusive);
        int refundCount = financeMapper.countRefundsByStoreAndDate(storeId, startTime, endExclusive);

        Map<String, MethodAmount> paymentByMethod = financeMapper.sumPaidByStoreDateGroupByMethod(storeId, startTime, endExclusive)
                .stream().collect(Collectors.toMap(MethodAmount::method, m -> m));
        Map<String, MethodAmount> refundByMethod = financeMapper.sumRefundByStoreDateGroupByMethod(storeId, startTime, endExclusive)
                .stream().collect(Collectors.toMap(MethodAmount::method, m -> m));

        List<MethodBreakdown> byMethod = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            MethodAmount p = paymentByMethod.get(pm.getCode());
            MethodAmount r = refundByMethod.get(pm.getCode());
            BigDecimal pAmt = p != null ? nz(p.amount()) : BigDecimal.ZERO;
            BigDecimal rAmt = r != null ? nz(r.amount()) : BigDecimal.ZERO;
            int pCnt = p != null ? p.count() : 0;
            int rCnt = r != null ? r.count() : 0;
            // Only include methods that have activity
            if (pCnt == 0 && rCnt == 0) continue;
            MethodBreakdown mb = new MethodBreakdown();
            mb.setMethod(pm.getCode());
            mb.setPaymentAmount(pAmt.setScale(2, RoundingMode.HALF_UP));
            mb.setRefundAmount(rAmt.setScale(2, RoundingMode.HALF_UP));
            mb.setNetAmount(pAmt.subtract(rAmt).setScale(2, RoundingMode.HALF_UP));
            mb.setPaymentCount(pCnt);
            mb.setRefundCount(rCnt);
            byMethod.add(mb);
        }

        int currentUnpaidWorkOrderCount = financeMapper.countCurrentUnpaidWorkOrders(storeId);
        int currentPartialPaidWorkOrderCount = financeMapper.countCurrentPartialPaidWorkOrders(storeId);

        CashierReportResponse response = new CashierReportResponse();
        response.setDate(date);
        response.setStoreId(storeId);
        response.setTotalPaymentAmount(totalPaymentAmount.setScale(2, RoundingMode.HALF_UP));
        response.setTotalRefundAmount(totalRefundAmount.setScale(2, RoundingMode.HALF_UP));
        response.setNetAmount(netAmount);
        response.setPaymentCount(paymentCount);
        response.setRefundCount(refundCount);
        response.setByMethod(byMethod);
        response.setCurrentUnpaidWorkOrderCount(currentUnpaidWorkOrderCount);
        response.setCurrentPartialPaidWorkOrderCount(currentPartialPaidWorkOrderCount);
        return response;
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

    private BigDecimal nz(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
