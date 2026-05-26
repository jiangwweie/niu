export interface InventoryStock {
  partId: string | number;
  partCode: string;
  partName: string;
  partSource?: string;
  storeId?: string | number;
  actualQty: number;
  availableQty: number;
  reservedQty: number;
  lastChangedAt?: string;
}

export interface InboundRequest {
  partId?: number | string;
  quantity: number;
  unitCost?: number;
  barcode?: string;
  code?: string;
  locationRemark?: string;
  reason?: string;
  remark?: string;
}

export interface InboundResponse {
  partId: number | string;
  partCode: string;
  partName: string;
  actualQty: number;
  availableQty: number;
  reservedQty: number;
  flowId: number | string;
  operatedAt: string;
}
