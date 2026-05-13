import request from '@/utils/request';
import type { OfficialSettlementQuery, OfficialSettlementRecord } from '@/types/officialSettlement';

interface OfficialAfterSalesListResp {
  id: number;
  storeId: number;
  workOrderId: number;
  workOrderNo: string;
  customerNameSnapshot: string;
  customerPhoneSnapshot: string;
  vehicleModelSnapshot: string;
  frameNoSnapshot: string;
  receivedAmount: number;
  workOrderStatus: string;
  officialOrderNo: string | null;
  settlementAmount: number | null;
  settlementStatus: string;
  settlementTime: string | null;
}

export interface OfficialAfterSalesDetailResp {
  id: number;
  storeId: number;
  workOrderId: number;
  workOrderNo: string;
  officialAfterSales: boolean;
  officialOrderNo: string | null;
  settlementAmount: number | null;
  settlementStatus: string;
  settlementTime: string | null;
  operatorId: number | null;
  settlementRemark: string | null;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

interface PageResp<T> {
  records: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}

function adaptOfficialAfterSalesList(resp: OfficialAfterSalesListResp): OfficialSettlementRecord {
  return {
    id: String(resp.id),
    orderNo: resp.workOrderNo || '',
    customerName: resp.customerNameSnapshot || '',
    phone: resp.customerPhoneSnapshot || '',
    scooterModel: resp.vehicleModelSnapshot || '',
    vin: resp.frameNoSnapshot || '',
    officialOrderNo: resp.officialOrderNo || undefined,
    orderStatus: resp.workOrderStatus || '',
    customerActualPaid: resp.receivedAmount || 0,
    customerActualRefund: 0,
    receivableAmount: 0,
    settlementStatus: (resp.settlementStatus as OfficialSettlementRecord['settlementStatus']) || 'PENDING',
    settlementAmount: resp.settlementAmount ?? undefined,
    settlementTime: resp.settlementTime || undefined,
  };
}

export async function getOfficialAfterSalesList(params: OfficialSettlementQuery): Promise<{ records: OfficialSettlementRecord[]; total: number }> {
  const backendParams: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  if (params.workOrderNo) backendParams.workOrderNo = params.workOrderNo;
  if (params.officialOrderNo) backendParams.officialOrderNo = params.officialOrderNo;
  if (params.settlementStatus) backendParams.settlementStatus = params.settlementStatus;
  if (params.startTime) backendParams.startTime = params.startTime;
  if (params.endTime) backendParams.endTime = params.endTime;

  const page: PageResp<OfficialAfterSalesListResp> = await request.get('/api/admin/official-after-sales', { params: backendParams });
  return {
    records: page.records.map(adaptOfficialAfterSalesList),
    total: page.total,
  };
}

export async function getOfficialAfterSalesDetail(workOrderId: number): Promise<OfficialAfterSalesDetailResp> {
  return await request.get(`/api/admin/work-orders/${workOrderId}/official-after-sales`);
}

export async function saveOfficialOrderInfo(workOrderId: number, body: { officialOrderNo: string; remark?: string }): Promise<number> {
  return await request.post(`/api/admin/work-orders/${workOrderId}/official-after-sales/order-info`, body);
}

export async function markOfficialSettled(workOrderId: number, body: { settlementAmount: number; remark?: string }): Promise<void> {
  return await request.post(`/api/admin/work-orders/${workOrderId}/official-after-sales/settle`, body);
}

export async function markNoSettlementRequired(workOrderId: number, body: { reason: string; remark?: string }): Promise<void> {
  return await request.post(`/api/admin/work-orders/${workOrderId}/official-after-sales/no-settlement-required`, body);
}
