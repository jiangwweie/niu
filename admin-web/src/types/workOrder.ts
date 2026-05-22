/* ── View-layer types (used by work-order page) ── */

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
  progressStatus?: string;
  progressStatusText?: string;
  cashierStatus?: string;
  cashierStatusText?: string;
  inventoryStatus?: string;
  inventoryStatusText?: string;
  receivableAmount: number;
  actualAmount: number; // Keep for backward compatibility, same as receivedAmount/netReceived
  paidAmount: number;
  refundedAmount: number;
  netReceived?: number;
  outstandingAmount?: number;
  refundableAmount?: number;
  noChargeReason?: string | null;
  noChargeRemark?: string | null;
  canMarkRepairDone?: boolean;
  canDeliver?: boolean;
  canCancel?: boolean;
  canRecordPayment?: boolean;
  canRecordRefund?: boolean;
  canRefundAfterDelivery?: boolean;
  isOfficial: boolean;
  officialOrderNo?: string;
  officialSettlementStatus?: string;
  officialSettlementAmount?: number;
  createdAt: string;
  remark?: string;
  repairItem?: string;
  chargeItems: WorkOrderChargeItem[];
}

export interface WorkOrderQuery {
  orderNo?: string;
  customerName?: string;
  phone?: string;
  vin?: string;
  status?: string;
  progressStatus?: string;
  cashierStatus?: string;
  isOfficial?: boolean | '';
  dateRange?: [string, string];
  pageNo: number;
  pageSize: number;
}

/* ── Backend response shapes ── */

export interface WorkOrderListResp {
  id: number;
  workOrderNo: string;
  customerNameSnapshot: string;
  vehicleModelSnapshot: string;
  status: string;
  progressStatus: string;
  progressStatusText: string;
  cashierStatus: string;
  cashierStatusText: string;
  inventoryStatus: string;
  inventoryStatusText: string;
  receivableAmount: number;
  receivedAmount: number;
  paymentTotal: number;
  refundTotal: number;
  netReceived: number;
  outstandingAmount: number;
  refundableAmount: number;
  createdAt: string;
  customerPhoneSnapshot: string;
  frameNoSnapshot: string;
  officialAfterSales: boolean;
  officialOrderNo: string | null;
}

export interface WorkOrderChargeItemResp {
  id: number;
  workOrderId: number;
  chargeType: string;
  itemName: string;
  partId: number | null;
  partCodeSnapshot: string;
  partNameSnapshot: string;
  partSourceSnapshot: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  lineAmount: number;
  costPriceSnapshot: number | null;
  lineCostAmount: number;
  inventoryAffecting: boolean;
  tempPart: boolean;
  status: string;
  remark: string;
}

export interface OfficialAfterSalesResp {
  id: number;
  storeId: number;
  workOrderId: number;
  workOrderNo: string;
  officialOrderNo: string;
  settlementAmount: number;
  settlementStatus: string;
  settlementTime: string | null;
  operatorId: number;
  settlementRemark: string;
  remark: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkOrderDetailResp {
  id: number;
  storeId: number;
  workOrderNo: string;
  customerId: number;
  vehicleId: number;
  customerNameSnapshot: string;
  customerPhoneSnapshot: string;
  vehicleModelSnapshot: string;
  frameNoSnapshot: string;
  batteryNoSnapshot: string | null;
  repairItem: string | null;
  status: string;
  progressStatus: string;
  progressStatusText: string;
  cashierStatus: string;
  cashierStatusText: string;
  inventoryStatus: string;
  inventoryStatusText: string;
  receivableAmount: number;
  receivedAmount: number;
  paymentTotal: number;
  refundTotal: number;
  netReceived: number;
  outstandingAmount: number;
  refundableAmount: number;
  noChargeReason: string | null;
  noChargeRemark: string | null;
  canMarkRepairDone: boolean;
  canDeliver: boolean;
  canCancel: boolean;
  canRecordPayment: boolean;
  canRecordRefund: boolean;
  canRefundAfterDelivery: boolean;
  remark: string | null;
  createdAt: string;
  chargeItems: WorkOrderChargeItemResp[];
  officialAfterSales: OfficialAfterSalesResp | null;
}
