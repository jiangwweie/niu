export interface WorkOrderChargeItem {
  id: string;
  type: 'PART' | 'LABOR' | 'OTHER';
  itemName: string;
  partCode?: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
  costAmount?: number;
  affectsInventory: boolean;
}

export interface WorkOrderRecord {
  id: string;
  orderNo: string;
  customerName: string;
  phone: string;
  scooterModel: string;
  vin: string;
  batteryNo?: string;
  status: string;
  receivableAmount: number;
  paidAmount: number;
  refundedAmount: number;
  actualAmount: number;
  isOfficial: boolean;
  officialOrderNo?: string;
  officialSettlementStatus?: string;
  officialSettlementAmount?: number;
  createdAt: string;
  remark?: string;
  chargeItems: WorkOrderChargeItem[];
}

export interface WorkOrderQuery {
  orderNo?: string;
  customerName?: string;
  phone?: string;
  vin?: string;
  status?: string;
  isOfficial?: boolean | '';
  dateRange?: [string, string];
  pageNo: number;
  pageSize: number;
}
