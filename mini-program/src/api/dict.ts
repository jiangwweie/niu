import { request } from '../utils/request';

export const getDictItems = (typeCode: string) => {
  return request<any[]>({
    url: `/api/staff/dict/types/${typeCode}/items`,
    method: 'GET',
    mockData: []
  });
};

export const dictItemLabels = (items: any[], fallback: string[] = []) => {
  const labels = (items || [])
    .filter(item => item && item.enabled !== false)
    .map(item => item.itemName || item.dictLabel || item.label || item.itemCode)
    .filter(Boolean);
  return labels.length > 0 ? labels : fallback;
};
