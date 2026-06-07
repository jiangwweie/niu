export interface WorkOrderItem {
  id: string | number;
  workOrderId?: string | number;
  chargeType: string;
  itemName: string;
  partId?: string | number;
  partCodeSnapshot?: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
  unit?: string;
  remark?: string;
}

/** 工单进度状态 */
export type ProgressStatus = 'DRAFT' | 'REPAIRING' | 'REPAIR_DONE' | 'DELIVERED' | 'CANCELLED';

/** 收银状态 */
export type CashierStatus =
  | 'NO_CHARGE'
  | 'UNPAID'
  | 'PARTIAL_PAID'
  | 'PAID'
  | 'REFUND_PENDING'
  | 'PARTIAL_REFUNDED'
  | 'REFUNDED';

/** 库存状态 */
export type InventoryStatus = 'NOT_RESERVED' | 'RESERVED' | 'CONSUMED' | 'RELEASED';

/** 旧状态（试运行期间残留） */
export type LegacyStatus = 'PENDING_ACCEPT' | 'ACCEPTED' | 'PART_ORDERED' | 'PART_ARRIVED' | 'SETTLED';

export interface WorkOrder {
  id: string | number;
  workOrderNo: string;
  /** @deprecated 后端仍返回兼容，页面优先使用 progressStatus */
  status: string;
  // --- M24B 新状态 ---
  progressStatus?: ProgressStatus;
  progressStatusText?: string;
  cashierStatus?: CashierStatus;
  cashierStatusText?: string;
  inventoryStatus?: InventoryStatus;
  inventoryStatusText?: string;
  // --- 客户/车辆 ---
  customerId?: number;
  vehicleId?: number;
  customerNameSnapshot: string;
  customerPhoneSnapshot?: string;
  vehicleModelSnapshot?: string;
  frameNoSnapshot?: string;
  batteryNoSnapshot?: string;
  repairItem?: string;
  remark?: string;
  // --- 金额字段 ---
  receivableAmount: number;
  receivedAmount: number;
  paymentTotal?: number;
  refundTotal?: number;
  netReceived?: number;
  outstandingAmount?: number;
  refundableAmount?: number;
  // --- 无需收款 ---
  noChargeReason?: string;
  noChargeRemark?: string;
  // --- 后端能力标志 ---
  canMarkRepairDone?: boolean;
  canDeliver?: boolean;
  canCancel?: boolean;
  canRecordPayment?: boolean;
  canRecordRefund?: boolean;
  canRefundAfterDelivery?: boolean;
  // --- 其他 ---
  createdAt: string;
  chargeItems?: WorkOrderItem[];
}

export interface CreateDraftWorkOrderRequest {
  customerId?: number;
  vehicleId?: number;
  customerNameSnapshot: string;
  customerPhoneSnapshot?: string;
  vehicleModelSnapshot?: string;
  frameNoSnapshot?: string;
  batteryNoSnapshot?: string;
  repairItem: string;
  remark?: string;
}

export interface UpdateDraftWorkOrderRequest {
  customerId?: number;
  vehicleId?: number;
  customerNameSnapshot?: string;
  customerPhoneSnapshot?: string;
  vehicleModelSnapshot?: string;
  frameNoSnapshot?: string;
  batteryNoSnapshot?: string;
  repairItem?: string;
  remark?: string;
}

export interface AddChargeItemRequest {
  chargeType: string;
  itemName: string;
  partId?: string | number;
  barcode?: string;
  code?: string;
  quantity: number;
  unit?: string;
  unitPrice: number;
  remark?: string;
}

export interface AddTempPartChargeRequest {
  source?: string;
  partName: string;
  officialPartNo?: string;
  barcode?: string;
  model?: string;
  categoryCode?: string;
  quantity: number;
  unit?: string;
  unitPrice: number;
  unitCost?: number;
  locationRemark?: string;
  remark?: string;
}

export interface AddTempPartChargeResponse {
  partId: string | number;
  chargeItemId: string | number;
}

export interface UpdateChargeItemRequest {
  itemName?: string;
  quantity: number;
  unit?: string;
  unitPrice?: number;
  remark?: string;
}

export interface SubmitWorkOrderRequest {
  remark?: string;
}

export interface CancelWorkOrderRequest {
  reason: string;
  remark?: string;
}

export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'UNIONPAY' | 'CASH' | 'OTHER';

export interface RecordPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  paidAt?: string;
  remark?: string;
}

export interface StaffPaymentRecordResponse {
  id: number | string;
  workOrderId: number | string;
  paymentNo: string;
  amount: number;
  paymentMethod: string;
  paidAt?: string;
  receiverId?: number | string;
  receiverName?: string;
  operatorId?: number | string;
  operatorName?: string;
  remark?: string;
}

export interface RecordRefundRequest {
  amount: number;
  refundMethod: PaymentMethod;
  refundedAt?: string;
  reason: string;
  remark?: string;
}

export interface StaffRefundRecordResponse {
  id: number | string;
  workOrderId: number | string;
  refundNo: string;
  amount: number;
  refundMethod: string;
  refundedAt?: string;
  operatorId?: number | string;
  operatorName?: string;
  reason?: string;
  remark?: string;
}

export interface MarkRepairDoneRequest {
  noChargeReason?: string;
  noChargeRemark?: string;
}

export interface DeliverWorkOrderRequest {
  remark?: string;
}

export interface AddNonInventoryChargeRequest {
  chargeType: 'LABOR' | 'OTHER';
  itemName: string;
  amount: number;
  reason: string;
  remark?: string;
}

/** @deprecated 已由 markRepairDone + deliver 替代，仅保留类型兼容 */
export interface SettleWorkOrderRequest {
  settledAt?: string;
  remark?: string;
}

/** @deprecated 已由 markRepairDone + deliver 替代，仅保留类型兼容 */
export interface StaffSettledWorkOrderResponse {
  workOrderId: number | string;
  workOrderNo: string;
  status: string;
  receivableAmount: number;
  receivedAmount: number;
  settledAt: string;
  settlerId: number | string;
}
