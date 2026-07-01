import { request } from '../utils/request';
import { PageResponse, ApiResponse } from '../types/common';
import {
  WorkOrder,
  CreateDraftWorkOrderRequest,
  UpdateDraftWorkOrderRequest,
  AddChargeItemRequest,
  AddTempPartChargeRequest,
  AddTempPartChargeResponse,
  UpdateChargeItemRequest,
  SubmitWorkOrderRequest,
  CancelWorkOrderRequest,
  RecordPaymentRequest,
  StaffPaymentRecordResponse,
  RecordRefundRequest,
  StaffRefundRecordResponse,
  MarkRepairDoneRequest,
  DeliverWorkOrderRequest,
  AddNonInventoryChargeRequest
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

export const addTempPartCharge = (workOrderId: string | number, data: AddTempPartChargeRequest) => {
  return request<AddTempPartChargeResponse>({
    url: `/api/staff/work-orders/${workOrderId}/temp-part-charge`,
    method: 'POST',
    data,
    mockData: { partId: Date.now(), chargeItemId: Date.now() + 1 },
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
    data: data || {},
    mockData: { id: workOrderId, progressStatus: 'REPAIRING', progressStatusText: '维修中' } as WorkOrder,
    showLoading: true
  });
};

export const cancelWorkOrder = (workOrderId: string | number, data: CancelWorkOrderRequest) => {
  const record = mockWorkOrders.records.find(w => w.id == workOrderId);
  if (record) {
    record.progressStatus = 'CANCELLED';
    record.progressStatusText = '已取消';
  }
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/cancel`,
    method: 'POST',
    data,
    mockData: { id: workOrderId, progressStatus: 'CANCELLED', progressStatusText: '已取消' } as WorkOrder,
    showLoading: true
  });
};

export const recordPayment = (workOrderId: string | number, data: RecordPaymentRequest) => {
  const record = mockWorkOrders.records.find(w => w.id == workOrderId);
  if (record) {
    record.receivedAmount = (record.receivedAmount || 0) + data.amount;
  }
  return request<StaffPaymentRecordResponse>({
    url: `/api/staff/work-orders/${workOrderId}/payments`,
    method: 'POST',
    data,
    mockData: { id: Date.now(), workOrderId, amount: data.amount, paymentMethod: data.paymentMethod } as StaffPaymentRecordResponse,
    showLoading: true
  });
};

export const recordRefund = (workOrderId: string | number, data: RecordRefundRequest) => {
  const record = mockWorkOrders.records.find(w => w.id == workOrderId);
  if (record) {
    record.receivedAmount = Math.max(0, (record.receivedAmount || 0) - data.amount);
  }
  return request<StaffRefundRecordResponse>({
    url: `/api/staff/work-orders/${workOrderId}/refunds`,
    method: 'POST',
    data,
    mockData: { id: Date.now(), workOrderId, amount: data.amount, refundMethod: data.refundMethod, reason: data.reason } as StaffRefundRecordResponse,
    showLoading: true
  });
};

export const markRepairDone = (workOrderId: string | number, data?: MarkRepairDoneRequest) => {
  const record = mockWorkOrders.records.find(w => w.id == workOrderId);
  if (record) {
    record.progressStatus = 'REPAIR_DONE';
    record.progressStatusText = '维修完成';
  }
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/mark-repair-done`,
    method: 'POST',
    data: data || {},
    mockData: { id: workOrderId, progressStatus: 'REPAIR_DONE', progressStatusText: '维修完成' } as WorkOrder,
    showLoading: true
  });
};

export const deliverWorkOrder = (workOrderId: string | number, data?: DeliverWorkOrderRequest) => {
  const record = mockWorkOrders.records.find(w => w.id == workOrderId);
  if (record) {
    record.progressStatus = 'DELIVERED';
    record.progressStatusText = '已交付';
  }
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/deliver`,
    method: 'POST',
    data: data || {},
    mockData: { id: workOrderId, progressStatus: 'DELIVERED', progressStatusText: '已交付' } as WorkOrder,
    showLoading: true
  });
};

export const addNonInventoryCharge = (workOrderId: string | number, data: AddNonInventoryChargeRequest) => {
  return request<WorkOrder>({
    url: `/api/staff/work-orders/${workOrderId}/non-inventory-charges`,
    method: 'POST',
    data,
    mockData: undefined,
    showLoading: true
  });
};
