import request from '@/utils/request';
import { setSearchParam } from '@/utils/searchParams';

/** Backend PartQueryResponse / PartDetailResponse shape */
interface PartResp {
  id: number;
  storeId: number;
  partCode: string;
  officialPartNo: string;
  partName: string;
  model: string;
  source: string;
  categoryCode: string;
  referenceCostPrice: number;
  defaultSalePrice?: number;
  defaultBarcode?: string;
  locationRemark: string;
  createSource: string;
  status: string;
  remark: string;
  canDelete?: boolean;
  deleteReasons?: string[];
  deleteBlockReasonSummary?: string;
  actualQty?: number;
  availableQty?: number;
  reservedQty?: number;
  inventoryFlowCount?: number;
  workOrderChargeItemCount?: number;
  archived?: boolean;
  hasHistoryReference?: boolean;
}

interface PartDeleteCheckResp {
  canDelete: boolean;
  reasons: string[];
  stockSummary: {
    actualQty: number;
    availableQty: number;
    reservedQty: number;
  };
  referenceSummary: {
    inventoryFlowCount: number;
    workOrderChargeItemCount: number;
    sampleWorkOrderIds: number[];
  };
}

/** View-compatible part row (what the table template binds to) */
export interface PartViewRecord {
  id: string;
  partCode: string;
  partName: string;
  source: string;
  officialCode: string;
  model: string;
  category: string;
  costPrice: number;
  salePrice: number | null;
  barcode: string;
  location: string;
  status: boolean;
  remark: string;
  canDelete?: boolean;
  deleteReasons?: string[];
  deleteBlockReasonSummary?: string;
  actualQty?: number;
  availableQty?: number;
  reservedQty?: number;
  inventoryFlowCount?: number;
  workOrderChargeItemCount?: number;
  archived?: boolean;
  hasHistoryReference?: boolean;
}

/** Pagination params for list endpoint */
export interface PartListParams {
  partCode?: string;
  partName?: string;
  officialPartNo?: string;
  barcode?: string;
  model?: string;
  categoryCode?: string;
  source?: string;
  enabled?: boolean;
  pageNo: number;
  pageSize: number;
}

/** View-specific query with search filters (used by the parts page) */
export interface PartViewQuery {
  partCode?: string;
  partName?: string;
  source?: string;
  officialCode?: string;
  barcode?: string;
  model?: string;
  category?: string;
  status?: boolean | string;
  pageNo: number;
  pageSize: number;
}

function adaptPart(resp: PartResp): PartViewRecord {
  return {
    id: String(resp.id),
    partCode: resp.partCode,
    partName: resp.partName,
    source: resp.source?.toLowerCase() || '',
    officialCode: resp.officialPartNo || '',
    model: resp.model || '',
    category: resp.categoryCode || '',
    costPrice: resp.referenceCostPrice || 0,
    salePrice: resp.defaultSalePrice ?? null,
    barcode: resp.defaultBarcode || '',
    location: resp.locationRemark || '',
    status: resp.status === 'ENABLED',
    remark: resp.remark || '',
    canDelete: resp.canDelete,
    deleteReasons: resp.deleteReasons || [],
    deleteBlockReasonSummary: resp.deleteBlockReasonSummary || '',
    actualQty: resp.actualQty ?? 0,
    availableQty: resp.availableQty ?? 0,
    reservedQty: resp.reservedQty ?? 0,
    inventoryFlowCount: resp.inventoryFlowCount ?? 0,
    workOrderChargeItemCount: resp.workOrderChargeItemCount ?? 0,
    archived: resp.archived ?? false,
    hasHistoryReference: resp.hasHistoryReference ?? false,
  };
}

/** Backend paginated response shape */
interface PageResp<T> {
  records: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}

/**
 * GET /api/admin/parts
 * Fetches paginated parts list from the real backend.
 * Transforms backend field names to match the view template bindings.
 */
export async function getPartsList(params: PartViewQuery): Promise<{
  records: PartViewRecord[];
  total: number;
}> {
  const backendParams: PartListParams = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  setSearchParam(backendParams, 'partCode', params.partCode);
  setSearchParam(backendParams, 'partName', params.partName);
  setSearchParam(backendParams, 'officialPartNo', params.officialCode);
  setSearchParam(backendParams, 'barcode', params.barcode);
  setSearchParam(backendParams, 'model', params.model);
  setSearchParam(backendParams, 'categoryCode', params.category);
  if (params.source) backendParams.source = params.source.toUpperCase();
  if (params.status !== undefined && params.status !== '') {
    backendParams.enabled = params.status === true || params.status === 'true';
  }

  const page: PageResp<PartResp> = await request.get('/api/admin/parts', { params: backendParams });
  return {
    records: page.records.map(adaptPart),
    total: page.total,
  };
}

/**
 * GET /api/admin/parts/{partId}
 * Fetches a single part detail.
 */
export async function getPartDetail(partId: string | number): Promise<PartViewRecord> {
  const resp: PartResp = await request.get(`/api/admin/parts/${partId}`);
  return adaptPart(resp);
}

/**
 * POST /api/admin/parts/official
 * Creates an official part. Returns ApiResponse&lt;PartCreateResponse&gt; (partId, partCode, defaultBarcode, source, name, enabled).
 */
export function createOfficialPart(body: {
  partName: string;
  officialPartNo: string;
  model?: string;
  categoryCode?: string;
  referenceCostPrice?: number;
  defaultSalePrice?: number;
  defaultBarcode?: string;
  externalBarcode?: string;
  locationRemark?: string;
  remark?: string;
}) {
  return request.post('/api/admin/parts/official', body);
}

/**
 * POST /api/admin/parts/third-party
 * Creates a third-party part. Returns ApiResponse&lt;PartCreateResponse&gt; (partId, partCode, defaultBarcode, source, name, enabled).
 */
export function createThirdPartyPart(body: {
  partName: string;
  model?: string;
  categoryCode?: string;
  referenceCostPrice?: number;
  defaultSalePrice?: number;
  defaultBarcode?: string;
  externalBarcode?: string;
  locationRemark?: string;
  remark?: string;
}) {
  return request.post('/api/admin/parts/third-party', body);
}

/**
 * POST /api/admin/parts/{partId}/enable
 */
export function enablePart(partId: string | number) {
  return request.post(`/api/admin/parts/${partId}/enable`);
}

/**
 * POST /api/admin/parts/{partId}/disable
 */
export function disablePart(partId: string | number) {
  return request.post(`/api/admin/parts/${partId}/disable`);
}

export function deletePart(partId: string | number) {
  return request.delete(`/api/admin/parts/${partId}`);
}

export function getPartDeleteCheck(partId: string | number): Promise<PartDeleteCheckResp> {
  return request.get(`/api/admin/parts/${partId}/delete-check`);
}

/**
 * PUT /api/admin/parts/{partId}
 * Updates a part. Returns void.
 */
export function updatePart(partId: string | number, body: {
  partName: string;
  officialPartNo?: string;
  model?: string;
  categoryCode?: string;
  referenceCostPrice?: number;
  defaultSalePrice?: number;
  defaultBarcode?: string;
  externalBarcode?: string;
  locationRemark?: string;
  remark?: string;
}) {
  return request.put(`/api/admin/parts/${partId}`, body);
}
