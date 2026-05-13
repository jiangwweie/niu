import request from '@/utils/request';

/** Known dict type codes (no backend list endpoint exists yet) */
export const DICT_TYPE_OPTIONS = [
  { code: 'WORK_ORDER_STATUS', label: '工单状态' },
  { code: 'PAYMENT_METHOD', label: '支付方式' },
  { code: 'PART_SOURCE', label: '配件来源' },
  { code: 'PART_CATEGORY', label: '配件分类' },
  { code: 'INVENTORY_FLOW_TYPE', label: '库存流水类型' },
  { code: 'OFFICIAL_SETTLEMENT_STATUS', label: '官方结算状态' },
  { code: 'REIMBURSEMENT_STATUS', label: '报销状态' },
] as { code: string; label: string }[];

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
 * Get dict type options for the sidebar menu.
 * No backend endpoint exists yet — returns a hardcoded list.
 */
export function getDictionaryTypes() {
  return DICT_TYPE_OPTIONS;
}

/**
 * GET /api/admin/dict/types/{typeCode}/items
 * Fetches dictionary items for a given type code.
 */
export async function getDictionaryItems(typeCode: string): Promise<DictViewRow[]> {
  const items: DictItemResp[] = await request.get(`/api/admin/dict/types/${typeCode}/items`);
  return items.map(adaptDictItem);
}
