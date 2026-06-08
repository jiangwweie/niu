import { request } from '../utils/request';
import { ApiResponse } from '../types/common';

export interface CustomerSearchResult {
  id: number;
  name: string;
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
  return request<CustomerSearchResult[]>({
    url: `/api/staff/customers/search?keyword=${encodeURIComponent(keyword)}`,
    method: 'GET'
  });
};

export const searchVehicles = (keyword: string, customerId?: number | null) => {
  const customerParam = customerId ? `&customerId=${customerId}` : '';
  return request<VehicleSearchResult[]>({
    url: `/api/staff/vehicles/search?keyword=${encodeURIComponent(keyword)}${customerParam}`,
    method: 'GET'
  });
};
