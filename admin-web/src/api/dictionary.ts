import request from '@/utils/request';

/** Backend DictTypeResponse shape */
interface DictTypeResp {
  typeCode: string;
  typeName: string;
  enabled: boolean;
}

/** Frontend dict type option (sidebar menu items) */
export interface DictTypeOption {
  code: string;
  label: string;
}

/** Backend DictItemResponse shape */
interface DictItemResp {
  itemCode: string;
  itemName: string;
  sortOrder: number;
  enabled: boolean;
}

/** View-compatible dictionary row (what the table template binds to) */
export interface DictViewRow {
  dictCode: string;
  dictLabel: string;
  sort: number;
  enabled: boolean;
  isSystem: boolean;
}

function adaptDictType(resp: DictTypeResp): DictTypeOption {
  return { code: resp.typeCode, label: resp.typeName };
}

function adaptDictItem(resp: DictItemResp): DictViewRow {
  return {
    dictCode: resp.itemCode,
    dictLabel: resp.itemName,
    sort: resp.sortOrder,
    enabled: resp.enabled,
    isSystem: false,
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
