import request from '@/utils/request';
import type { RefundQuery, RefundRecord } from '@/types/refund';

/** Backend RefundQueryResponse shape */
interface RefundResp {
  id: number;
  workOrderId: number;
  workOrderNo: string;
  customerNameSnapshot: string;
  refundNo: string;
  amount: number;
  refundMethod: string;
  refundedAt: string;
  operatorId: number | null;
  operatorName?: string | null;
  reason: string | null;
  remark: string | null;
}

function adaptRefund(resp: RefundResp): RefundRecord {
  return {
    id: resp.id,
    refundNo: resp.refundNo || '',
    workOrderNo: resp.workOrderNo || '',
    customerName: resp.customerNameSnapshot || '',
    amount: resp.amount || 0,
    refundMethod: resp.refundMethod || '',
    refundedAt: resp.refundedAt || '',
    operatorId: resp.operatorId,
    operatorName: resp.operatorName || undefined,
    reason: resp.reason || '',
    remark: resp.remark || undefined,
  };
}

interface PageResp<T> {
  records: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}

/**
 * GET /api/admin/refunds
 * Fetches paginated refund list from the real backend.
 */
export async function getRefundList(params: RefundQuery): Promise<{
  records: RefundRecord[];
  total: number;
}> {
  const backendParams: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  if (params.workOrderNo) backendParams.workOrderNo = params.workOrderNo;
  if (params.customerName) backendParams.customerName = params.customerName;
  if (params.refundMethod) backendParams.refundMethod = params.refundMethod;
  if (params.startTime) backendParams.startTime = params.startTime;
  if (params.endTime) backendParams.endTime = params.endTime;

  const page: PageResp<RefundResp> = await request.get('/api/admin/refunds', { params: backendParams });
  return {
    records: page.records.map(adaptRefund),
    total: page.total,
  };
}
