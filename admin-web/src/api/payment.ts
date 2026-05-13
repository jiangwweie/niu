import request from '@/utils/request';
import type { PaymentQuery, PaymentRecord } from '@/types/payment';

/** Backend PaymentQueryResponse shape */
interface PaymentResp {
  id: number;
  workOrderId: number;
  workOrderNo: string;
  customerNameSnapshot: string;
  paymentNo: string;
  amount: number;
  paymentMethod: string;
  paidAt: string;
  receiverId: number | null;
  operatorId: number | null;
  remark: string | null;
}

function adaptPayment(resp: PaymentResp): PaymentRecord {
  return {
    id: resp.id,
    paymentNo: resp.paymentNo || '',
    workOrderNo: resp.workOrderNo || '',
    customerName: resp.customerNameSnapshot || '',
    amount: resp.amount || 0,
    paymentMethod: resp.paymentMethod || '',
    paidAt: resp.paidAt || '',
    receiverId: resp.receiverId,
    operatorId: resp.operatorId,
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
 * GET /api/admin/payments
 * Fetches paginated payment list from the real backend.
 */
export async function getPaymentList(params: PaymentQuery): Promise<{
  records: PaymentRecord[];
  total: number;
}> {
  const backendParams: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  if (params.workOrderNo) backendParams.workOrderNo = params.workOrderNo;
  if (params.customerName) backendParams.customerName = params.customerName;
  if (params.paymentMethod) backendParams.paymentMethod = params.paymentMethod;
  if (params.startTime) backendParams.startTime = params.startTime;
  if (params.endTime) backendParams.endTime = params.endTime;

  const page: PageResp<PaymentResp> = await request.get('/api/admin/payments', { params: backendParams });
  return {
    records: page.records.map(adaptPayment),
    total: page.total,
  };
}
