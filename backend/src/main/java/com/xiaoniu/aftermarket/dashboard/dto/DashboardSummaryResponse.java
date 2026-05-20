package com.xiaoniu.aftermarket.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class DashboardSummaryResponse {

    private Integer todayWorkOrderCount;
    private Integer pendingSettleWorkOrderCount;
    private Integer pendingReimbursementCount;
    private Integer lowStockPartCount;

    private BigDecimal monthCustomerIncome;
    private BigDecimal monthOfficialIncome;
    private BigDecimal monthPartsCost;
    private BigDecimal monthReimbursementCost;
    private BigDecimal monthProfit;

    private List<RecentWorkOrderItem> recentWorkOrders;
    private PendingActionsSummary pendingActions;

    @Data
    public static class RecentWorkOrderItem {
        private Long id;
        private String workOrderNo;
        private String customerName;
        private String status;
        private BigDecimal receivableAmount;
        private BigDecimal receivedAmount;
        private String createdAt;
    }

    @Data
    public static class PendingActionsSummary {
        private Integer pendingSettleCount;
        private Integer pendingReimbursementCount;
        private Integer pendingOfficialSettlementCount;
    }
}
