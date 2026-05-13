export interface RefundRecord {
  id: string;
  refundNo: string;
  orderNo: string;
  customerName: string;
  amount: number;
  method: 'wechat' | 'alipay' | 'unionpay' | 'cash';
  refundTime: string;
  operator: string;
  reason: string;
  remark?: string;
  createdAt: string;
}

export interface RefundQuery {
  refundNo?: string;
  orderNo?: string;
  customerName?: string;
  method?: string;
  dateRange?: [string, string];
  operator?: string;
  reason?: string;
  pageNo: number;
  pageSize: number;
}
