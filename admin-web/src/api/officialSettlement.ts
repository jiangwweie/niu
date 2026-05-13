import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { OfficialSettlementQuery, OfficialSettlementRecord } from '@/types/officialSettlement';
import { mockOfficialSettlements } from '@/mock/officialSettlement';

export const getSettlementList = (params: OfficialSettlementQuery): Promise<BaseHttpResponse<PaginatedResult<OfficialSettlementRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockOfficialSettlements];
      
      if (params.orderNo) filtered = filtered.filter(p => p.orderNo.includes(params.orderNo!));
      if (params.officialOrderNo) filtered = filtered.filter(p => p.officialOrderNo && p.officialOrderNo.includes(params.officialOrderNo!));
      if (params.customerName) filtered = filtered.filter(p => p.customerName.includes(params.customerName!));
      if (params.settlementStatus !== undefined && params.settlementStatus !== '') filtered = filtered.filter(p => p.settlementStatus === params.settlementStatus);
      
      if (params.hasAmount !== undefined && params.hasAmount !== '') {
        filtered = filtered.filter(p => {
          const hasAmt = p.settlementAmount !== undefined && p.settlementAmount !== null;
          return params.hasAmount ? hasAmt : !hasAmt;
        });
      }
      
      if (params.dateRange && params.dateRange.length === 2) {
        filtered = filtered.filter(p => p.settlementTime && p.settlementTime >= params.dateRange![0] && p.settlementTime <= params.dateRange![1]);
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
