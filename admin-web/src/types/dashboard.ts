export interface RecentWorkOrderItem {
  id: number;
  workOrderNo: string;
  customerName: string | null;
  status: string;
  receivableAmount: number;
  receivedAmount: number;
  createdAt: string;
}

export interface PendingActionsSummary {
  pendingSettleCount: number;
  pendingReimbursementCount: number;
  pendingOfficialSettlementCount: number;
}

export interface DashboardSummaryResponse {
  todayWorkOrderCount: number;
  pendingSettleWorkOrderCount: number;
  pendingReimbursementCount: number;
  lowStockPartCount: number;
  monthCustomerIncome: number;
  monthOfficialIncome: number;
  monthPartsCost: number;
  monthReimbursementCost: number;
  monthProfit: number;
  recentWorkOrders: RecentWorkOrderItem[];
  pendingActions: PendingActionsSummary;
}
