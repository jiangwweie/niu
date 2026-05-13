export interface FinanceSummary {
  customerPaidIncome: number;
  officialSettlementIncome: number;
  totalIncome: number;
  
  partsIncome: number;
  laborIncome: number;
  otherIncome: number;
  
  partsCost: number;
  reimbursementCost: number;
  totalCost: number;
  
  profit: number;
  profitMargin: string;
}

export interface FinanceDetailRecord {
  id: string;
  dateOrMonth: string;
  customerPaidIncome: number;
  officialSettlementIncome: number;
  partsIncome: number;
  laborIncome: number;
  otherIncome: number;
  partsCost: number;
  reimbursementCost: number;
  profit: number;
}

export interface FinanceQuery {
  reportType: 'DAILY' | 'MONTHLY' | 'CUSTOM';
  dateRange?: [string, string];
  pageNo: number;
  pageSize: number;
}
