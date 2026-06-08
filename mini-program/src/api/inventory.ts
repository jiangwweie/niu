import { request } from '../utils/request';
import { PageResponse } from '../types/common';
import { InventoryStock, InboundRequest, InboundResponse } from '../types/inventory';
import { mockInventory } from '../mock/inventory';

export const getInventoryStocks = (params?: any) => {
  return request<PageResponse<InventoryStock>>({
    url: '/api/staff/inventory/stocks',
    method: 'GET',
    data: params,
    mockData: mockInventory,
    showLoading: true
  });
};

export const getInventoryStockDetail = (partId: string) => {
  return request<InventoryStock>({
    url: `/api/staff/inventory/stocks/${partId}`,
    method: 'GET',
    mockData: mockInventory.records.find(i => i.partId === partId) || null,
    showLoading: true
  });
};

export const inboundInventory = (data: InboundRequest) => {
  return request<InboundResponse>({
    url: '/api/staff/inventory/inbound',
    method: 'POST',
    data,
    mockData: {
      partId: data.partId,
      partCode: 'MOCK-PART',
      partName: 'Mock 配件',
      actualQty: data.quantity,
      availableQty: data.quantity,
      reservedQty: 0,
      flowId: `FLOW${Date.now()}`,
      operatedAt: new Date().toISOString()
    },
    showLoading: true
  });
};

export const createPartAndInbound = (data: {
  source: string;
  partName: string;
  officialPartNo?: string;
  externalBarcode?: string;
  model?: string;
  categoryCode?: string;
  costPrice?: number;
  salePrice?: number;
  inboundQuantity: number;
  unitCost?: number;
  locationRemark?: string;
  reason?: string;
  remark?: string;
}) => {
  return request<InboundResponse>({
    url: '/api/staff/inventory/inbound/create-part-and-inbound',
    method: 'POST',
    data,
    showLoading: true
  });
};
