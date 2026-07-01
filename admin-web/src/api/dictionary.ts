import request from '@/utils/request';

/** Backend DictTypeResponse shape */
interface DictTypeResp {
  typeCode: string;
  typeName: string;
  enabled: boolean;
  editMode?: string;
}

/** Frontend dict type option (sidebar menu items) */
export interface DictTypeOption {
  code: string;
  label: string;
  editMode?: string;
}

/** Backend DictItemResponse shape */
interface DictItemResp {
  id: number;
  itemCode: string;
  itemName: string;
  sortOrder: number;
  enabled: boolean;
  scope?: 'SYSTEM' | 'STORE';
  storeId?: number | null;
  systemItem?: boolean;
  editable?: boolean;
}

/** View-compatible dictionary row (what the table template binds to) */
export interface DictViewRow {
  id: number;
  dictCode: string;
  dictLabel: string;
  sort: number;
  enabled: boolean;
  isSystem: boolean;
  scope: 'SYSTEM' | 'STORE';
  storeId?: number | null;
  editable: boolean;
}

function adaptDictType(resp: DictTypeResp): DictTypeOption {
  return { code: resp.typeCode, label: resp.typeName, editMode: resp.editMode };
}

function adaptDictItem(resp: DictItemResp): DictViewRow {
  return {
    id: resp.id,
    dictCode: resp.itemCode,
    dictLabel: resp.itemName,
    sort: resp.sortOrder,
    enabled: resp.enabled,
    isSystem: Boolean(resp.systemItem),
    scope: (resp.scope || 'SYSTEM') as 'SYSTEM' | 'STORE',
    storeId: resp.storeId,
    editable: Boolean(resp.editable),
  };
}

/**
 * GET /api/admin/dict/types
 * Fetches all enabled dictionary types from the backend.
 */
export async function getDictionaryTypes(): Promise<DictTypeOption[]> {
  const types: DictTypeResp[] = await request.get('/api/admin/dict/types');
  return types.map(adaptDictType);
}

/**
 * GET /api/admin/dict/types/{typeCode}/items
 * Fetches dictionary items for a given type code.
 */
export async function getDictionaryItems(typeCode: string): Promise<DictViewRow[]> {
  const items: DictItemResp[] = await request.get(`/api/admin/dict/types/${typeCode}/items`);
  return items.map(adaptDictItem);
}

export interface SaveDictItemPayload {
  itemCode?: string;
  itemName: string;
  sortOrder?: number;
  scope?: 'SYSTEM' | 'STORE';
  status?: 'ENABLED' | 'DISABLED';
  remark?: string;
}

export async function createDictionaryItem(typeCode: string, data: SaveDictItemPayload): Promise<DictViewRow> {
  const item: DictItemResp = await request.post(`/api/admin/dict/types/${typeCode}/items`, data);
  return adaptDictItem(item);
}

export async function updateDictionaryItem(id: number, data: SaveDictItemPayload): Promise<DictViewRow> {
  const item: DictItemResp = await request.put(`/api/admin/dict/items/${id}`, data);
  return adaptDictItem(item);
}

export async function deleteDictionaryItem(id: number): Promise<void> {
  await request.delete(`/api/admin/dict/items/${id}`);
}
