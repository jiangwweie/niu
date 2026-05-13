import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { ReimbursementQuery, ReimbursementRecord } from '@/types/reimbursement';
import { mockReimbursements } from '@/mock/reimbursement';

export const getReimbursementList = (params: ReimbursementQuery): Promise<BaseHttpResponse<PaginatedResult<ReimbursementRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockReimbursements];
      
      if (params.reimbursementNo) filtered = filtered.filter(p => p.reimbursementNo.includes(params.reimbursementNo!));
      if (params.applicant) filtered = filtered.filter(p => p.applicant.includes(params.applicant!));
      if (params.keyword) filtered = filtered.filter(p => p.purpose.includes(params.keyword!));
      if (params.status !== undefined && params.status !== '') filtered = filtered.filter(p => p.status === params.status);
      if (params.confirmer) filtered = filtered.filter(p => p.confirmer && p.confirmer.includes(params.confirmer!));
      
      if (params.dateRange && params.dateRange.length === 2) {
        filtered = filtered.filter(p => p.createdAt >= params.dateRange![0] && p.createdAt <= params.dateRange![1]);
      }
      
      const pageNo = params.pageNo || 1;
      const pageSize = params.pageSize || 10;
      const start = (pageNo - 1) * pageSize;
      const pagedData = filtered.slice(start, start + pageSize);

      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: {
          records: pagedData,
          total: filtered.length,
          pageNo,
          pageSize
        }
      });
    }, 300);
  });
};
