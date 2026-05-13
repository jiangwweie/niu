export interface PaymentRecord {
  id: string;
  paymentNo: string;
  orderNo: string;
  customerName: string;
  amount: number;
  method: 'wechat' | 'alipay' | 'unionpay' | 'cash';
  paymentTime: string;
  payee: string;
  remark?: string;
  createdAt: string;
}

export interface PaymentQuery {
  paymentNo?: string;
  orderNo?: string;
  customerName?: string;
  method?: string;
  dateRange?: [string, string];
  payee?: string;
  pageNo: number;
  pageSize: number;
}
