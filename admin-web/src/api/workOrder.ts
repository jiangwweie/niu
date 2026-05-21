import request from '@/utils/request';
import type { PaginatedResult } from '@/types';
import type {
  WorkOrderQuery,
  WorkOrderRecord,
  WorkOrderListResp,
  WorkOrderDetailResp,
  WorkOrderChargeItemResp,
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
  return {
    id: String(resp.id),
    orderNo: resp.workOrderNo,
    customerName: resp.customerNameSnapshot,
    phone: resp.customerPhoneSnapshot || '',
    scooterModel: resp.vehicleModelSnapshot,
    vin: resp.frameNoSnapshot || '',
    status: resp.status,
    receivableAmount: resp.receivableAmount,
    actualAmount: resp.receivedAmount,
    paidAmount: 0,
    refundedAmount: 0,
    isOfficial: resp.officialAfterSales,
    officialOrderNo: resp.officialOrderNo || undefined,
    createdAt: resp.createdAt,
    chargeItems: [],
  };
}

function adaptWorkOrderDetail(resp: WorkOrderDetailResp): WorkOrderRecord {
  const ps = resp.paymentSummary;
  return {
    id: String(resp.id),
    orderNo: resp.workOrderNo,
    customerName: resp.customerNameSnapshot,
    phone: resp.customerPhoneSnapshot || '',
    scooterModel: resp.vehicleModelSnapshot,
    vin: resp.frameNoSnapshot || '',
    batteryNo: resp.batteryNoSnapshot || undefined,
    status: resp.status,
    receivableAmount: resp.receivableAmount,
    actualAmount: resp.receivedAmount,
    paidAmount: ps?.paymentTotal ?? 0,
    refundedAmount: ps?.refundTotal ?? 0,
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
  if (params.orderNo) backendParams.workOrderNo = params.orderNo;
  if (params.customerName) backendParams.customerName = params.customerName;
  if (params.phone) backendParams.customerPhone = params.phone;
  if (params.vin) backendParams.vehicleFrameNo = params.vin;
  if (params.status) backendParams.status = params.status;
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

/** POST /api/admin/work-orders/{id}/settle */
export async function settleWorkOrder(
  workOrderId: string | number,
  data: { remark?: string },
) {
  return await request.post(`/api/admin/work-orders/${workOrderId}/settle`, data);
}
