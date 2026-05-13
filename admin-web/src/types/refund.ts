/** View-compatible refund row (what the table template binds to) */
export interface RefundRecord {
  id: number;
  refundNo: string;
  workOrderNo: string;
  customerName: string;
  amount: number;
  refundMethod: string;
  refundedAt: string;
  operatorId: number | null;
  reason: string;
  remark?: string;
}

/** View-layer query params for the refund list page */
export interface RefundQuery {
  workOrderNo?: string;
  customerName?: string;
  refundMethod?: string;
  startTime?: string;
  endTime?: string;
  pageNo: number;
  pageSize: number;
}
