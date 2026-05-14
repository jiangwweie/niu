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

export interface WorkOrder {
  id: string | number;
  workOrderNo: string;
  status: string;
  customerNameSnapshot: string;
  customerPhoneSnapshot?: string;
  vehicleModelSnapshot?: string;
  frameNoSnapshot?: string;
  batteryNoSnapshot?: string;
  repairItem?: string;
  remark?: string;
  receivableAmount: number;
  receivedAmount: number;
  createdAt: string;
  chargeItems?: WorkOrderItem[];
}

export interface CreateDraftWorkOrderRequest {
  customerNameSnapshot: string;
  customerPhoneSnapshot?: string;
  vehicleModelSnapshot?: string;
  frameNoSnapshot?: string;
  batteryNoSnapshot?: string;
  repairItem: string;
  remark?: string;
}

export interface UpdateDraftWorkOrderRequest {
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
  quantity: number;
  unit?: string;
  unitPrice: number;
  remark?: string;
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
  operatorId?: number | string;
  remark?: string;
}
