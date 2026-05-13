import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { FinanceSummary, FinanceDetailRecord, FinanceQuery } from '@/types/finance';
import { mockDailySummaries, mockMonthlySummaries, mockTotalSummary } from '@/mock/finance';

export const getFinanceSummary = (): Promise<BaseHttpResponse<FinanceSummary>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: mockTotalSummary
      });
    }, 300);
  });
};

export const getFinanceList = (params: FinanceQuery): Promise<BaseHttpResponse<PaginatedResult<FinanceDetailRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let data = params.reportType === 'MONTHLY' ? mockMonthlySummaries : mockDailySummaries;
      
      const pageNo = params.pageNo || 1;
      const pageSize = params.pageSize || 10;
      const start = (pageNo - 1) * pageSize;
      const pagedData = data.slice(start, start + pageSize);

      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: {
          records: pagedData,
          total: data.length,
          pageNo,
          pageSize
        }
      });
    }, 300);
  });
};
