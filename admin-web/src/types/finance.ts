export interface FinanceReportResponse {
  storeId: number;
  periodStart: string;
  periodEnd: string;
  
  customerIncome: number;
  officialIncome: number;
  partsCost: number;
  reimbursementCost: number;
  
  totalIncome: number;
  totalCost: number;
  profit: number;
  
  settledWorkOrderCount: number;
  confirmedReimbursementCount: number;
}

export interface DailyFinanceQuery {
  date?: string;
}

export interface MonthlyFinanceQuery {
  year: number;
  month: number;
}

export interface RangeFinanceQuery {
  startDate: string;
  endDate: string;
}

export interface FinanceQuery {
  reportType: 'DAILY' | 'MONTHLY' | 'CUSTOM';
  date?: string; // used for DAILY
  year?: number; // used for MONTHLY
  month?: number; // used for MONTHLY
  dateRange?: [string, string]; // used for CUSTOM
}
