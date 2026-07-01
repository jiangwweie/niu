import request from '@/utils/request';
import { setSearchParam } from '@/utils/searchParams';
import type { PaginatedResult } from '@/types';
import type {
  WorkOrderQuery,
  WorkOrderRecord,
  WorkOrderListResp,
  WorkOrderDetailResp,
  WorkOrderChargeItemResp,
  UpdateDraftWorkOrderRequest,
} from '@/types/workOrder';

/* ── Adapters: backend → view ── */

function adaptChargeItem(resp: WorkOrderChargeItemResp) {
  return {
    id: String(resp.id),
    type: resp.chargeType as WorkOrderRecord['chargeItems'][number]['type'],
    itemName: resp.itemName,
    partCode: resp.partCodeSnapshot || undefined,
    quantity: resp.quantity,
    unitPrice: resp.unitPrice,
    lineAmount: resp.lineAmount,
    costAmount: resp.lineCostAmount ?? undefined,
    affectsInventory: resp.inventoryAffecting,
  };
}

function adaptWorkOrderList(resp: WorkOrderListResp): WorkOrderRecord {
  const draftNoCharge = resp.progressStatus === 'DRAFT' && resp.cashierStatus === 'NO_CHARGE';
  return {
    id: String(resp.id),
    orderNo: resp.workOrderNo,
    customerName: resp.customerNameSnapshot,
    phone: resp.customerPhoneSnapshot || '',
    scooterModel: resp.vehicleModelSnapshot,
    vin: resp.frameNoSnapshot || '',
    status: resp.status,
    progressStatus: resp.progressStatus,
    progressStatusText: resp.progressStatusText,
    cashierStatus: resp.cashierStatus,
    cashierStatusText: draftNoCharge ? '待录入费用' : resp.cashierStatusText,
    inventoryStatus: resp.inventoryStatus,
    inventoryStatusText: resp.inventoryStatusText,
    receivableAmount: resp.receivableAmount,
    actualAmount: resp.receivedAmount, // Keep actualAmount as receivedAmount for backward compatibility
    paidAmount: resp.paymentTotal ?? 0,
    refundedAmount: resp.refundTotal ?? 0,
    netReceived: resp.netReceived ?? 0,
    outstandingAmount: resp.outstandingAmount ?? 0,
    refundableAmount: resp.refundableAmount ?? 0,
    isOfficial: resp.officialAfterSales,
    officialOrderNo: resp.officialOrderNo || undefined,
    createdAt: resp.createdAt,
    chargeItems: [],
  };
}

function adaptWorkOrderDetail(resp: WorkOrderDetailResp): WorkOrderRecord {
  const draftNoCharge = resp.progressStatus === 'DRAFT' && resp.cashierStatus === 'NO_CHARGE';
  return {
    id: String(resp.id),
    customerId: resp.customerId,
    vehicleId: resp.vehicleId,
    orderNo: resp.workOrderNo,
    customerName: resp.customerNameSnapshot,
    phone: resp.customerPhoneSnapshot || '',
    scooterModel: resp.vehicleModelSnapshot,
    vin: resp.frameNoSnapshot || '',
    batteryNo: resp.batteryNoSnapshot || undefined,
    status: resp.status,
    progressStatus: resp.progressStatus,
    progressStatusText: resp.progressStatusText,
    cashierStatus: resp.cashierStatus,
    cashierStatusText: draftNoCharge
      ? ((resp.chargeItems || []).length > 0 || resp.receivableAmount > 0 ? '草稿未提交' : '待录入费用')
      : resp.cashierStatusText,
    inventoryStatus: resp.inventoryStatus,
    inventoryStatusText: resp.inventoryStatusText,
    receivableAmount: resp.receivableAmount,
    actualAmount: resp.receivedAmount,
    paidAmount: resp.paymentTotal ?? 0,
    refundedAmount: resp.refundTotal ?? 0,
    netReceived: resp.netReceived ?? 0,
    outstandingAmount: resp.outstandingAmount ?? 0,
    refundableAmount: resp.refundableAmount ?? 0,
    noChargeReason: resp.noChargeReason,
    noChargeRemark: resp.noChargeRemark,
    canMarkRepairDone: resp.canMarkRepairDone,
    canDeliver: resp.canDeliver,
    canCancel: resp.canCancel,
    canRecordPayment: resp.canRecordPayment,
    canRecordRefund: resp.canRecordRefund,
    canRefundAfterDelivery: resp.canRefundAfterDelivery,
    isOfficial: resp.officialAfterSales !== null,
    officialOrderNo: resp.officialAfterSales?.officialOrderNo || undefined,
    officialSettlementStatus: resp.officialAfterSales?.settlementStatus || undefined,
    officialSettlementAmount: resp.officialAfterSales?.settlementAmount || undefined,
    createdAt: resp.createdAt,
    remark: resp.remark || undefined,
    repairItem: resp.repairItem || undefined,
    chargeItems: (resp.chargeItems || []).map(adaptChargeItem),
  };
}

/* ── API calls ── */

/** GET /api/admin/work-orders */
export async function getWorkOrderList(
  params: WorkOrderQuery,
): Promise<PaginatedResult<WorkOrderRecord>> {
  const backendParams: Record<string, string | number | boolean> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  setSearchParam(backendParams, 'workOrderNo', params.orderNo);
  setSearchParam(backendParams, 'customerName', params.customerName);
  setSearchParam(backendParams, 'customerPhone', params.phone);
  setSearchParam(backendParams, 'vehicleFrameNo', params.vin);
  setSearchParam(backendParams, 'scooterModel', params.scooterModel);
  if (params.partId) backendParams.partId = params.partId;
  if (params.status) backendParams.status = params.status;
  if (params.progressStatus) backendParams.status = params.progressStatus;
  if (params.isOfficial === true) backendParams.officialOnly = true;
  if (params.dateRange?.[0]) backendParams.startTime = params.dateRange[0];
  if (params.dateRange?.[1]) backendParams.endTime = params.dateRange[1];

  const page: PaginatedResult<WorkOrderListResp> = await request.get(
    '/api/admin/work-orders',
    { params: backendParams },
  );
  return {
    records: page.records.map(adaptWorkOrderList),
    total: page.total,
    pageNo: page.pageNo,
    pageSize: page.pageSize,
  };
}

/** GET /api/admin/work-orders/{id} */
export async function getWorkOrderDetail(
  id: string | number,
): Promise<WorkOrderRecord> {
  const resp: WorkOrderDetailResp = await request.get(
    `/api/admin/work-orders/${id}`,
  );
  return adaptWorkOrderDetail(resp);
}

/** PUT /api/admin/work-orders/{id}/draft */
export async function updateDraftWorkOrder(
  workOrderId: string | number,
  data: UpdateDraftWorkOrderRequest,
) {
  return await request.put(`/api/admin/work-orders/${workOrderId}/draft`, data);
}

/* ── Cashier APIs ── */

/** GET /api/admin/work-orders/{id}/payments */
export async function getWorkOrderPayments(workOrderId: string | number) {
  const data: any[] = await request.get(`/api/admin/work-orders/${workOrderId}/payments`);
  return data;
}

/** GET /api/admin/work-orders/{id}/refunds */
export async function getWorkOrderRefunds(workOrderId: string | number) {
  const data: any[] = await request.get(`/api/admin/work-orders/${workOrderId}/refunds`);
  return data;
}

/** POST /api/admin/work-orders/{id}/payments */
export async function recordPayment(
  workOrderId: string | number,
  data: { amount: number; paymentMethod: string; receiverId: number; remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/payments`, data);
}

/** POST /api/admin/work-orders/{id}/refunds */
export async function recordRefund(
  workOrderId: string | number,
  data: { amount: number; refundMethod: string; reason: string; remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/refunds`, data);
}

/** POST /api/admin/work-orders/{id}/mark-repair-done */
export async function markRepairDone(
  workOrderId: string | number,
  data?: { noChargeReason?: string; noChargeRemark?: string; remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/mark-repair-done`, data);
}

/** POST /api/admin/work-orders/{id}/deliver */
export async function deliverWorkOrder(
  workOrderId: string | number,
  data?: { noChargeReason?: string; noChargeRemark?: string; remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/deliver`, data);
}

/** POST /api/admin/work-orders/{id}/non-inventory-charges */
export async function addNonInventoryCharge(
  workOrderId: string | number,
  data: { chargeType: 'LABOR' | 'OTHER'; itemName: string; unitPrice: number; quantity: number; reason: string; remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/non-inventory-charges`, data);
}

/** POST /api/admin/work-orders/{id}/cancel */
export async function cancelWorkOrder(
  workOrderId: string | number,
  data: { reason: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/cancel`, data);
}

/** DELETE /api/admin/work-orders/{id} */
export async function deleteDraftWorkOrder(workOrderId: string | number) {
  return await request.delete(`/api/admin/work-orders/${workOrderId}`);
}
