import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { RefundQuery, RefundRecord } from '@/types/refund';
import { mockRefunds } from '@/mock/refund';

export const getRefundList = (params: RefundQuery): Promise<BaseHttpResponse<PaginatedResult<RefundRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockRefunds];
      
      if (params.refundNo) filtered = filtered.filter(p => p.refundNo.includes(params.refundNo!));
      if (params.orderNo) filtered = filtered.filter(p => p.orderNo.includes(params.orderNo!));
      if (params.customerName) filtered = filtered.filter(p => p.customerName.includes(params.customerName!));
      if (params.method !== undefined && params.method !== '') filtered = filtered.filter(p => p.method === params.method);
      if (params.operator) filtered = filtered.filter(p => p.operator.includes(params.operator!));
      if (params.reason) filtered = filtered.filter(p => p.reason.includes(params.reason!));
      
      if (params.dateRange && params.dateRange.length === 2) {
        filtered = filtered.filter(p => p.refundTime >= params.dateRange![0] && p.refundTime <= params.dateRange![1]);
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
