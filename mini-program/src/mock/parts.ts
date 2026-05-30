import { Part } from '../types/parts';

export const mockParts = {
  records: [
    {
      id: 100,
      partCode: 'P-NIU-001',
      partName: '小牛原装前刹车片 (N系列)',
      source: 'OFFICIAL',
      officialPartNo: 'N-BK-001',
      model: 'NQi/MQi',
      categoryCode: 'BRAKE',
      costPrice: 58.00,
      salePrice: 85.00
    },
    {
      id: 101,
      partCode: 'P-3RD-002',
      partName: '第三方通用后视镜',
      source: 'THIRD_PARTY',
      model: '通用',
      categoryCode: 'ACCESSORY',
      costPrice: 30.00,
      salePrice: 45.00
    }
  ] as Part[],
  pageNo: 1,
  pageSize: 10,
  total: 2
};
