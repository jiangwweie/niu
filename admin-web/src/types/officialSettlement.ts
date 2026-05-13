export interface OfficialSettlementRecord {
  id: string;
  orderNo: string;
  customerName: string;
  phone: string;
  scooterModel: string;
  vin: string;
  officialOrderNo?: string;
  orderStatus: string;
  customerActualPaid: number;
  customerActualRefund: number;
  receivableAmount: number;
  settlementStatus: 'PENDING' | 'SETTLED' | 'NOT_REQUIRED' | 'NOT_RECORDED';
  settlementAmount?: number;
  settlementTime?: string;
  remark?: string;
  createdAt: string;
}

export interface OfficialSettlementQuery {
  orderNo?: string;
  officialOrderNo?: string;
  customerName?: string;
  settlementStatus?: string;
  dateRange?: [string, string];
  hasAmount?: boolean | '';
  pageNo: number;
  pageSize: number;
}
