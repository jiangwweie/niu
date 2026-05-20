import { request } from '../utils/request';
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
  customerId: number;
  customerName: string;
}

export const searchCustomers = (keyword: string) => {
  return request<CustomerSearchResult[]>({
    url: `/api/staff/customers/search?keyword=${encodeURIComponent(keyword)}`,
    method: 'GET',
    showLoading: true
  });
};

export const searchVehicles = (keyword: string) => {
  return request<VehicleSearchResult[]>({
    url: `/api/staff/vehicles/search?keyword=${encodeURIComponent(keyword)}`,
    method: 'GET',
    showLoading: true
  });
};
