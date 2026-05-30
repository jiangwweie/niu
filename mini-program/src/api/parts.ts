import { request } from '../utils/request';
import { PageResponse } from '../types/common';
import { Part, PartLookupResult } from '../types/parts';
import { mockParts } from '../mock/parts';

export const getParts = (params?: any) => {
  return request<PageResponse<Part>>({
    url: '/api/staff/parts',
    method: 'GET',
    data: params,
    mockData: mockParts,
    showLoading: true
  });
};

export const getPartDetail = (partId: string) => {
  return request<Part>({
    url: `/api/staff/parts/${partId}`,
    method: 'GET',
    mockData: mockParts.records.find(p => p.id === partId) || null,
    showLoading: true
  });
};

export const lookupPartByCode = (code: string) => {
  const matched = mockParts.records.find(p =>
    p.partCode === code ||
    p.officialPartNo === code ||
    p.defaultBarcode === code
  );
  return request<PartLookupResult>({
    url: '/api/staff/parts/lookup',
    method: 'GET',
    data: { code },
    mockData: matched ? {
      matched: true,
      partId: matched.id,
      partCode: matched.partCode,
      officialPartNo: matched.officialPartNo,
      defaultBarcode: matched.defaultBarcode,
      source: matched.source,
      name: matched.partName,
      model: matched.model,
      category: matched.categoryCode,
      costPrice: matched.costPrice,
      salePrice: matched.salePrice,
      actualQty: 0,
      availableQty: 0,
      reservedQty: 0
    } : { matched: false },
    showLoading: true
  });
};
