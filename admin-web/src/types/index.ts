export interface UserInfo {
  username: string;
  role: string;
  storeId: string;
  storeName: string;
}

export interface PaginatedResult<T> {
  total: number;
  records: T[];
  pageNo: number;
  pageSize: number;
}

export interface BaseHttpResponse<T> {
  code: string | number;
  message: string;
  data: T;
  traceId?: string | null;
}

export * from './dashboard';
export * from './workOrder';
export * from './parts';
export * from './inventory';
export * from './payment';
export * from './refund';
export * from './officialSettlement';
export * from './reimbursement';
export * from './finance';
export * from './dictionary';
export * from './userPermission';
export * from './exportCenter';
export * from './store';


