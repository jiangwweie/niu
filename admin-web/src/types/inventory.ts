export interface InventoryRecord {
  id: string;
  partCode: string;
  partName: string;
  source: 'official' | 'third_party';
  actualQty: number;
  availableQty: number;
  reservedQty: number;
  warningThreshold: number;
  location: string;
  lastUpdated: string;
}

export interface InventoryQuery {
  partCode?: string;
  partName?: string;
  source?: 'official' | 'third_party' | '';
  location?: string;
  isWarning?: boolean | '';
  pageNo: number;
  pageSize: number;
}

export interface InventoryLogRecord {
  id: string;
  flowNo: string;
  partCode: string;
  partName: string;
  type: 'INBOUND' | 'RESERVE' | 'RELEASE' | 'CONSUME' | 'ADJUST';
  changeQty: number;
  beforeQty: number;
  afterQty: number;
  businessSource: string;
  operator: string;
  operatedAt: string;
  remark?: string;
}
