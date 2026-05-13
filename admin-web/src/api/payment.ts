import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { PaymentQuery, PaymentRecord } from '@/types/payment';
import { mockPayments } from '@/mock/payment';

export const getPaymentList = (params: PaymentQuery): Promise<BaseHttpResponse<PaginatedResult<PaymentRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockPayments];
      
      if (params.paymentNo) filtered = filtered.filter(p => p.paymentNo.includes(params.paymentNo!));
      if (params.orderNo) filtered = filtered.filter(p => p.orderNo.includes(params.orderNo!));
      if (params.customerName) filtered = filtered.filter(p => p.customerName.includes(params.customerName!));
      if (params.method !== undefined && params.method !== '') filtered = filtered.filter(p => p.method === params.method);
      if (params.payee) filtered = filtered.filter(p => p.payee.includes(params.payee!));
      
      if (params.dateRange && params.dateRange.length === 2) {
        filtered = filtered.filter(p => p.paymentTime >= params.dateRange![0] && p.paymentTime <= params.dateRange![1]);
      }
      
      const page = params.pageNo || 1;
      const pageSize = params.pageSize || 10;
      const start = (page - 1) * pageSize;
      const pagedData = filtered.slice(start, start + pageSize);

      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: {
          records: pagedData,
          total: filtered.length,
          page,
          pageSize
        }
      });
    }, 300);
  });
};
