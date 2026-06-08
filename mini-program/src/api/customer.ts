import { request } from '../utils/request';
import { normalizeSearchParam } from '../utils/searchParams';
import { ApiResponse } from '../types/common';

export interface CustomerSearchResult {
  id: number;
  customerName: string;
  phone: string;
}

export interface VehicleSearchResult {
  id: number;
  frameNo: string;
  model: string;
  batteryNo?: string;
  customerId: number;
  customerName: string;
  customerPhone?: string;
}

export const searchCustomers = (keyword: string) => {
  const normalizedKeyword = normalizeSearchParam(keyword);
  if (!normalizedKeyword) {
    return Promise.resolve<ApiResponse<CustomerSearchResult[]>>({
      code: 'SUCCESS',
      message: 'success',
      data: []
    });
  }
  return request<CustomerSearchResult[]>({
    url: `/api/staff/customers/search?keyword=${encodeURIComponent(normalizedKeyword)}`,
    method: 'GET'
  });
};

export const searchVehicles = (keyword: string, customerId?: number | null) => {
  const normalizedKeyword = normalizeSearchParam(keyword);
  if (!normalizedKeyword) {
    return Promise.resolve<ApiResponse<VehicleSearchResult[]>>({
      code: 'SUCCESS',
      message: 'success',
      data: []
    });
  }
  const customerParam = customerId ? `&customerId=${customerId}` : '';
  return request<VehicleSearchResult[]>({
    url: `/api/staff/vehicles/search?keyword=${encodeURIComponent(normalizedKeyword)}${customerParam}`,
    method: 'GET'
  });
};
