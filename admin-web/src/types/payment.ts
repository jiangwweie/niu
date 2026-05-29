/** View-compatible payment row (what the table template binds to) */
export interface PaymentRecord {
  id: number;
  paymentNo: string;
  workOrderNo: string;
  customerName: string;
  amount: number;
  paymentMethod: string;
  paidAt: string;
  receiverId: number | null;
  receiverName?: string;
  operatorId: number | null;
  operatorName?: string;
  remark?: string;
}

/** View-layer query params for the payment list page */
export interface PaymentQuery {
  workOrderNo?: string;
  customerName?: string;
  paymentMethod?: string;
  startTime?: string;
  endTime?: string;
  pageNo: number;
  pageSize: number;
}
