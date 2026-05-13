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
  settlementStatus: 'PENDING' | 'SETTLED' | 'NOT_REQUIRED';
  settlementAmount?: number;
  settlementTime?: string;
  remark?: string;
}

export interface OfficialSettlementQuery {
  workOrderNo?: string;
  officialOrderNo?: string;
  settlementStatus?: string;
  startTime?: string;
  endTime?: string;
  pageNo: number;
  pageSize: number;
}
