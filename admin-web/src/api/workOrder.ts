import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { WorkOrderRecord, WorkOrderQuery } from '@/types/workOrder';
import { mockWorkOrders } from '@/mock/workOrder';

/**
 * 获取工单列表
 */
export const getWorkOrderList = (params: WorkOrderQuery): Promise<BaseHttpResponse<PaginatedResult<WorkOrderRecord>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockWorkOrders];
      
      if (params.orderNo) {
        filtered = filtered.filter(item => item.orderNo.includes(params.orderNo!));
      }
      if (params.customerName) {
        filtered = filtered.filter(item => item.customerName.includes(params.customerName!));
      }
      if (params.phone) {
        filtered = filtered.filter(item => item.phone.includes(params.phone!));
      }
      if (params.vin) {
        filtered = filtered.filter(item => item.vin.includes(params.vin!));
      }
      if (params.status) {
        filtered = filtered.filter(item => item.status === params.status);
      }
      if (params.isOfficial !== undefined && params.isOfficial !== '') {
        filtered = filtered.filter(item => item.isOfficial === params.isOfficial);
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
    }, 400);
  });
};

/**
 * 获取工单详情
 */
export const getWorkOrderDetail = (id: string | number): Promise<BaseHttpResponse<WorkOrderRecord>> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const order = mockWorkOrders.find(item => item.id === id || item.orderNo === id);
      if (order) {
        resolve({
          code: 'SUCCESS',
          message: 'success',
          data: order
        });
      } else {
        reject(new Error('Work order not found'));
      }
    }, 300);
  });
};

/**
 * 创建工单
 */
export const createWorkOrder = (data: any): Promise<BaseHttpResponse<any>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: null
      });
    }, 300);
  });
};
