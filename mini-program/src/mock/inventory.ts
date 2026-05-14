import { InventoryStock } from '../types/inventory';

export const mockInventory = {
  records: [
    {
      partId: 100,
      partCode: 'P-NIU-001',
      partName: '小牛原装前刹车片 (N系列)',
      partSource: 'OFFICIAL',
      storeId: 1,
      actualQty: 100,
      availableQty: 98,
      reservedQty: 2,
      lastChangedAt: '2026-05-14T08:00:00Z'
    },
    {
      partId: 101,
      partCode: 'P-3RD-002',
      partName: '第三方通用后视镜',
      partSource: 'THIRD_PARTY',
      storeId: 1,
      actualQty: 10,
      availableQty: 10,
      reservedQty: 0,
      lastChangedAt: '2026-05-13T14:30:00Z'
    }
  ] as InventoryStock[],
  pageNo: 1,
  pageSize: 10,
  total: 2
};
