import request from '@/utils/request';
import type { PaginatedResult } from '@/types';
import type {
  InventoryQuery,
  InventoryRecord,
  InventoryLogRecord,
  InventoryStockResp,
  InventoryFlowResp,
  InventoryInboundBody,
  InventoryAdjustBody,
} from '@/types/inventory';

/* ── Adapters: backend → view ── */

function adaptStock(resp: InventoryStockResp): InventoryRecord {
  return {
    id: String(resp.id),
    partId: resp.partId,
    partCode: resp.partCode,
    partName: resp.partName,
    source: resp.partSource || '',
    partStatus: resp.partStatus || '',
    actualQty: resp.actualQty,
    availableQty: resp.availableQty,
    reservedQty: resp.reservedQty,
    lastChangedAt: resp.lastChangedAt || '',
    inventoryStateCode: resp.inventoryStateCode || '',
    inventoryStateTag: resp.inventoryStateTag || '',
    archived: resp.archived ?? false,
    canUseForNewBusiness: resp.canUseForNewBusiness ?? true,
    hasHistoryReference: resp.hasHistoryReference ?? false,
  };
}

function adaptFlow(resp: InventoryFlowResp): InventoryLogRecord {
  return {
    id: String(resp.id),
    flowType: resp.flowType,
    quantityChange: resp.quantityDelta ?? resp.quantityChange,
    partCode: resp.partCode,
    partName: resp.partName,
    operatorId: resp.operatorId,
    operatorName: resp.operatorName || (resp.operatorId ? `员工 #${resp.operatorId}` : ''),
    remark: resp.remark || '',
    createdAt: resp.operatedAt ?? resp.createdAt,
    actualBefore: resp.actualBefore ?? 0,
    actualAfter: resp.actualAfter ?? 0,
    availableBefore: resp.availableBefore ?? 0,
    availableAfter: resp.availableAfter ?? 0,
    reservedBefore: resp.reservedBefore ?? 0,
    reservedAfter: resp.reservedAfter ?? 0,
    businessType: resp.businessType || '',
    businessId: resp.businessId || '',
    unitCost: resp.unitCost ?? 0,
  };
}

/* ── API calls ── */

/** GET /api/admin/inventory/stocks */
export async function getInventoryList(
  params: InventoryQuery,
): Promise<PaginatedResult<InventoryRecord>> {
  const backendParams: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  if (params.keyword) backendParams.keyword = params.keyword;
  if (params.partCode) backendParams.partCode = params.partCode;
  if (params.partName) backendParams.partName = params.partName;
  if (params.source) backendParams.source = params.source;
  if (params.view) backendParams.view = params.view;

  const page: PaginatedResult<InventoryStockResp> = await request.get(
    '/api/admin/inventory/stocks',
    { params: backendParams },
  );
  return {
    records: page.records.map(adaptStock),
    total: page.total,
    pageNo: page.pageNo,
    pageSize: page.pageSize,
  };
}

/** GET /api/admin/inventory/flows */
export async function getInventoryFlows(
  params: { partId?: number; partCode?: string; partName?: string; flowType?: string; pageNo: number; pageSize: number },
): Promise<PaginatedResult<InventoryLogRecord>> {
  const backendParams: Record<string, string | number> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  if (params.partId) backendParams.partId = params.partId;
  if (params.partCode) backendParams.partCode = params.partCode;
  if (params.flowType) backendParams.flowType = params.flowType;

  const page: PaginatedResult<InventoryFlowResp> = await request.get(
    '/api/admin/inventory/flows',
    { params: backendParams },
  );
  return {
    records: page.records.map(adaptFlow),
    total: page.total,
    pageNo: page.pageNo,
    pageSize: page.pageSize,
  };
}

/** POST /api/admin/inventory/inbound */
export function submitInbound(body: InventoryInboundBody) {
  return request.post('/api/admin/inventory/inbound', body);
}

/** POST /api/admin/inventory/adjust */
export function submitAdjust(body: InventoryAdjustBody) {
  return request.post('/api/admin/inventory/adjust', body);
}
