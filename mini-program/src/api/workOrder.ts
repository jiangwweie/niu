import { request } from '../utils/request';
import { PageResponse, ApiResponse } from '../types/common';
import { 
  WorkOrder, 
  CreateDraftWorkOrderRequest, 
  UpdateDraftWorkOrderRequest, 
  AddChargeItemRequest, 
  UpdateChargeItemRequest,
  SubmitWorkOrderRequest,
  CancelWorkOrderRequest,
  RecordPaymentRequest,
  StaffPaymentRecordResponse,
  RecordRefundRequest,
  StaffRefundRecordResponse,
  SettleWorkOrderRequest,
  StaffSettledWorkOrderResponse
} from '../types/workOrder';
import { mockWorkOrders } from '../mock/workOrder';

export const getWorkOrders = (params?: any) => {
  return request<PageResponse<WorkOrder>>({
    url: '/api/staff/work-orders',
    method: 'GET',
    data: params,
    mockData: mockWorkOrders,
    showLoading: true
  });
};

export const getWorkOrderDetail = (workOrderId: string | number) => {
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}`,
    method: 'GET',
    mockData: mockWorkOrders.records.find(w => w.id == workOrderId) || null,
    showLoading: true
  });
};

export const createDraftWorkOrder = (data: CreateDraftWorkOrderRequest) => {
  return request<WorkOrder>({
    url: '/api/staff/work-orders/drafts',
    method: 'POST',
    data,
    mockData: { id: Date.now() } as WorkOrder,
    showLoading: true
  });
};

export const updateDraftWorkOrder = (workOrderId: string | number, data: UpdateDraftWorkOrderRequest) => {
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/draft`,
    method: 'PUT',
    data,
    mockData: undefined,
    showLoading: true
  });
};

export const addChargeItem = (workOrderId: string | number, data: AddChargeItemRequest) => {
  return request<{ chargeItemId: number }>({
    url: `/api/staff/work-orders/${workOrderId}/charge-items`,
    method: 'POST',
    data,
    mockData: undefined,
    showLoading: true
  });
};

export const updateChargeItem = (workOrderId: string | number, chargeItemId: string | number, data: UpdateChargeItemRequest) => {
  return request<void>({
    url: `/api/staff/work-orders/${workOrderId}/charge-items/${chargeItemId}`,
    method: 'PUT',
    data,
    mockData: undefined,
    showLoading: true
  });
};

export const deleteChargeItem = (workOrderId: string | number, chargeItemId: string | number) => {
  return request<void>({
    url: `/api/staff/work-orders/${workOrderId}/charge-items/${chargeItemId}`,
    method: 'DELETE',
    mockData: undefined,
    showLoading: true
  });
};

export const submitWorkOrder = (workOrderId: string | number, data?: SubmitWorkOrderRequest) => {
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/submit`,
    method: 'POST',
    data,
    mockData: { id: workOrderId, status: 'PENDING_ACCEPT' } as WorkOrder,
    showLoading: true
  });
};

export const cancelWorkOrder = (workOrderId: string | number, data: CancelWorkOrderRequest) => {
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/cancel`,
    method: 'POST',
    data,
    mockData: { id: workOrderId, status: 'CANCELLED' } as WorkOrder,
    showLoading: true
  });
};

export const recordPayment = (workOrderId: string | number, data: RecordPaymentRequest) => {
  return request<StaffPaymentRecordResponse>({
    url: `/api/staff/work-orders/${workOrderId}/payments`,
    method: 'POST',
    data,
    mockData: { id: Date.now(), workOrderId, amount: data.amount, paymentMethod: data.paymentMethod } as StaffPaymentRecordResponse,
    showLoading: true
  });
};

export const recordRefund = (workOrderId: string | number, data: RecordRefundRequest) => {
  return request<StaffRefundRecordResponse>({
    url: `/api/staff/work-orders/${workOrderId}/refunds`,
    method: 'POST',
    data,
    mockData: { id: Date.now(), workOrderId, amount: data.amount, refundMethod: data.refundMethod, reason: data.reason } as StaffRefundRecordResponse,
    showLoading: true
  });
};

export const settleWorkOrder = (workOrderId: string | number, data?: SettleWorkOrderRequest) => {
  return request<StaffSettledWorkOrderResponse>({
    url: `/api/staff/work-orders/${workOrderId}/settle`,
    method: 'POST',
    data,
    mockData: { workOrderId, status: 'SETTLED', receivableAmount: 0, receivedAmount: 0, settledAt: new Date().toISOString(), settlerId: 1 } as StaffSettledWorkOrderResponse,
    showLoading: true
  });
};
