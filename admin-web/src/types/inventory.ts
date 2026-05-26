/* ── View-layer types (used by inventory page) ── */

export interface InventoryRecord {
  id: string;
  partId: number;
  partCode: string;
  partName: string;
  source: string;
  actualQty: number;
  availableQty: number;
  reservedQty: number;
  lastChangedAt: string;
}

export interface InventoryQuery {
  partCode?: string;
  partName?: string;
  source?: string;
  pageNo: number;
  pageSize: number;
}

export interface InventoryLogRecord {
  id: string;
  flowType: string;
  quantityChange: number;
  partCode: string;
  partName: string;
  operatorId: number;
  operatorName?: string;
  remark: string;
  createdAt: string;
  /* inventory before/after */
  actualBefore: number;
  actualAfter: number;
  availableBefore: number;
  availableAfter: number;
  reservedBefore: number;
  reservedAfter: number;
  /* business linkage */
  businessType: string;
  businessId: string;
  unitCost: number;
}

/* ── Backend response shapes ── */

export interface InventoryStockResp {
  id: number;
  storeId: number;
  partId: number;
  partCode: string;
  partName: string;
  partSource: string;
  actualQty: number;
  availableQty: number;
  reservedQty: number;
  lastFlowId: number | null;
  lastChangedAt: string | null;
}

export interface InventoryFlowResp {
  id: number;
  storeId: number;
  partId: number;
  partCode: string;
  partName: string;
  flowType: string;
  quantityDelta?: number;
  quantityChange: number;
  relatedOrderId: number | null;
  operatorId: number;
  operatorName?: string;
  remark: string;
  operatedAt?: string;
  createdAt: string;
  /* inventory before/after */
  actualBefore: number;
  actualAfter: number;
  availableBefore: number;
  availableAfter: number;
  reservedBefore: number;
  reservedAfter: number;
  /* business linkage */
  businessType: string;
  businessId: string;
  unitCost: number;
}

/* ── Request bodies ── */

export interface InventoryInboundBody {
  partId: number;
  quantity: number;
  unitCost?: number;
  reason?: string;
  remark?: string;
}

export interface InventoryAdjustBody {
  partId: number;
  quantityDelta: number;
  reason: string;
  remark?: string;
}
