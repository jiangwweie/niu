export interface ApiResponse<T = any> {
  code: string | number;
  message: string;
  data: T;
  traceId?: string;
}

export interface PageResponse<T> {
  records: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}
