import { request } from '../utils/request';
import { PageResponse } from '../types/common';
import { Part } from '../types/parts';
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
