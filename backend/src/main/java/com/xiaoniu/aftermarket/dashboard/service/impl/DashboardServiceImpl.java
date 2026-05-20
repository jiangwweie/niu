package com.xiaoniu.aftermarket.dashboard.service.impl;

import com.xiaoniu.aftermarket.dashboard.dto.DashboardSummaryResponse;
import com.xiaoniu.aftermarket.dashboard.dto.DashboardSummaryResponse.PendingActionsSummary;
import com.xiaoniu.aftermarket.dashboard.dto.DashboardSummaryResponse.RecentWorkOrderItem;
import com.xiaoniu.aftermarket.dashboard.mapper.DashboardMapper;
import com.xiaoniu.aftermarket.dashboard.mapper.RecentWorkOrderRow;
import com.xiaoniu.aftermarket.dashboard.service.DashboardService;
import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DashboardMapper dashboardMapper;
    private final FinanceService financeService;

    public DashboardServiceImpl(DashboardMapper dashboardMapper, FinanceService financeService) {
        this.dashboardMapper = dashboardMapper;
        this.financeService = financeService;
    }

    @Override
    public DashboardSummaryResponse getSummary(Long storeId) {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        LocalDateTime now = LocalDateTime.now();

        // Counts
        response.setTodayWorkOrderCount(dashboardMapper.countTodayWorkOrders(storeId, now));
        response.setPendingSettleWorkOrderCount(dashboardMapper.countPendingSettleWorkOrders(storeId));
        response.setPendingReimbursementCount(dashboardMapper.countPendingReimbursements(storeId));
        response.setLowStockPartCount(dashboardMapper.countLowStockParts(storeId));

        // Monthly financial figures - reuse FinanceService for consistency
        LocalDate today = LocalDate.now();
        FinanceReportResponse monthlyFinance = financeService.queryMonthly(storeId, today.getYear(), today.getMonthValue());
        response.setMonthCustomerIncome(monthlyFinance.getCustomerIncome());
        response.setMonthOfficialIncome(monthlyFinance.getOfficialIncome());
        response.setMonthPartsCost(monthlyFinance.getPartsCost());
        response.setMonthReimbursementCost(monthlyFinance.getReimbursementCost());
        response.setMonthProfit(monthlyFinance.getProfit());

        // Recent work orders
        List<RecentWorkOrderRow> recentRows = dashboardMapper.findRecentWorkOrders(storeId);
        List<RecentWorkOrderItem> recentList = new ArrayList<>();
        for (RecentWorkOrderRow row : recentRows) {
            RecentWorkOrderItem item = new RecentWorkOrderItem();
            item.setId(row.getId());
            item.setWorkOrderNo(row.getWorkOrderNo());
            item.setCustomerName(emptyToNull(row.getCustomerName()));
            item.setStatus(row.getStatus());
            item.setReceivableAmount(row.getReceivableAmount());
            item.setReceivedAmount(row.getReceivedAmount());
            LocalDateTime createdAt = row.getCreatedAt();
            item.setCreatedAt(createdAt != null ? createdAt.format(DISPLAY_FMT) : null);
            recentList.add(item);
        }
        response.setRecentWorkOrders(recentList);

        // Pending actions summary
        PendingActionsSummary pending = new PendingActionsSummary();
        pending.setPendingSettleCount(response.getPendingSettleWorkOrderCount());
        pending.setPendingReimbursementCount(response.getPendingReimbursementCount());
        pending.setPendingOfficialSettlementCount(dashboardMapper.countPendingOfficialSettlements(storeId));
        response.setPendingActions(pending);

        return response;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
