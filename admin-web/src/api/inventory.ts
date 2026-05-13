import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { InventoryQuery, InventoryRecord, InventoryLogRecord } from '@/types/inventory';
import { mockInventories, mockInventoryLogs } from '@/mock/inventory';

/**
 * 获取库存列表
 */
export const getInventoryList = (params: InventoryQuery): Promise<BaseHttpResponse<PaginatedResult<InventoryRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockInventories];
      
      if (params.partCode) filtered = filtered.filter(p => p.partCode.includes(params.partCode!));
      if (params.partName) filtered = filtered.filter(p => p.partName.includes(params.partName!));
      if (params.source !== undefined && params.source !== '') filtered = filtered.filter(p => p.source === params.source);
      if (params.location) filtered = filtered.filter(p => p.location.includes(params.location!));
      if (params.isWarning !== undefined && params.isWarning !== '') {
        filtered = filtered.filter(p => {
            const isWarn = p.availableQty < p.warningThreshold;
            return params.isWarning ? isWarn : !isWarn;
        });
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

/**
 * 获取库存流水
 */
export const getInventoryLogs = (partCode?: string): Promise<BaseHttpResponse<InventoryLogRecord[]>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const logs = partCode ? mockInventoryLogs.filter(log => log.partCode === partCode) : mockInventoryLogs;
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: logs
      });
    }, 300);
  });
}
