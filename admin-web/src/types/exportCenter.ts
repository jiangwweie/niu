export type ExportTypeCode = 'WORK_ORDER_LIST' | 'FINANCE_REPORT' | 'PROFIT_REPORT' | 'INVENTORY_REPORT' | 'REIMBURSEMENT_LEDGER';

export interface ExportTypeInfo {
  code: ExportTypeCode;
  name: string;
  description: string;
  targetRole: string;
  dataSourceDesc: string;
}

export type ExportTaskStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';

export interface ExportTask {
  id: string;
  taskNo: string;
  typeCode: ExportTypeCode;
  typeName: string;
  conditionSummary: string;
  creator: string;
  createTime: string;
  status: ExportTaskStatus;
  fileName?: string;
  remark?: string;
  
  // condition details for Drawer
  dateRange?: [string, string];
  workOrderStatus?: string;
  isOfficial?: boolean | string;
  reimbursementStatus?: string;
  inventorySource?: string;
}

export interface ExportTaskQuery {
  pageNo: number;
  pageSize: number;
  typeCode?: string;
  status?: string;
}
