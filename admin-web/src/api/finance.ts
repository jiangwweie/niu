import request from '@/utils/request';
import type { DailyFinanceQuery, MonthlyFinanceQuery, RangeFinanceQuery, FinanceReportResponse } from '@/types/finance';

export async function getDailyFinance(params: DailyFinanceQuery): Promise<FinanceReportResponse> {
  return await request.get('/api/admin/finance/daily', { params });
}

export async function getMonthlyFinance(params: MonthlyFinanceQuery): Promise<FinanceReportResponse> {
  return await request.get('/api/admin/finance/monthly', { params });
}

export async function getRangeFinance(params: RangeFinanceQuery): Promise<FinanceReportResponse> {
  return await request.get('/api/admin/finance/range', { params });
}
