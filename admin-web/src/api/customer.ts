import request from '@/utils/request';
import { setSearchParam } from '@/utils/searchParams';
import type { PaginatedResult } from '@/types';

export interface CustomerListItem {
  id: number;
  customerName: string;
  phone: string;
  remark: string;
  vehicleCount: number;
  lastRepairAt: string | null;
  createdAt: string;
}

export interface CustomerDetail {
  id: number;
  customerName: string;
  phone: string;
  remark: string;
  createdAt: string;
  vehicles: VehicleBrief[];
  recentWorkOrders: WorkOrderBrief[];
}

export interface VehicleBrief {
  id: number;
  model: string;
  frameNo: string;
  batteryNo: string;
  remark: string;
}

export interface WorkOrderBrief {
  id: number;
  workOrderNo: string;
  customerNameSnapshot: string;
  customerPhoneSnapshot: string;
  vehicleModelSnapshot: string;
  frameNoSnapshot: string;
  status: string;
  receivableAmount: number;
  receivedAmount: number;
  createdAt: string;
}

export interface VehicleListItem {
  id: number;
  customerId: number;
  customerName: string;
  customerPhone: string;
  model: string;
  frameNo: string;
  batteryNo: string;
  remark: string;
  lastRepairAt: string | null;
  createdAt: string;
}

export interface VehicleDetail {
  id: number;
  customerId: number;
  customerName: string;
  customerPhone: string;
  model: string;
  frameNo: string;
  batteryNo: string;
  remark: string;
  createdAt: string;
  recentWorkOrders: WorkOrderBrief[];
}

export interface CustomerQuery {
  keyword?: string;
  phone?: string;
  customerName?: string;
  pageNo: number;
  pageSize: number;
}

export interface VehicleQuery {
  keyword?: string;
  vin?: string;
  model?: string;
  customerPhone?: string;
  customerName?: string;
  pageNo: number;
  pageSize: number;
}

export async function getCustomerList(params: CustomerQuery): Promise<PaginatedResult<CustomerListItem>> {
  const backendParams: Record<string, unknown> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  setSearchParam(backendParams, 'keyword', params.keyword);
  setSearchParam(backendParams, 'phone', params.phone);
  setSearchParam(backendParams, 'customerName', params.customerName);
  return request.get('/api/admin/customers', { params: backendParams });
}

export async function getCustomerDetail(id: number): Promise<CustomerDetail> {
  return request.get(`/api/admin/customers/${id}`);
}

export async function createCustomer(data: { customerName: string; phone?: string; remark?: string }): Promise<number> {
  return request.post('/api/admin/customers', data);
}

export async function updateCustomer(id: number, data: { customerName: string; phone?: string; remark?: string }): Promise<void> {
  return request.put(`/api/admin/customers/${id}`, data);
}

export async function deleteCustomer(id: number): Promise<void> {
  return request.delete(`/api/admin/customers/${id}`);
}

export async function getVehicleList(params: VehicleQuery): Promise<PaginatedResult<VehicleListItem>> {
  const backendParams: Record<string, unknown> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  setSearchParam(backendParams, 'keyword', params.keyword);
  setSearchParam(backendParams, 'vin', params.vin);
  setSearchParam(backendParams, 'model', params.model);
  setSearchParam(backendParams, 'customerPhone', params.customerPhone);
  setSearchParam(backendParams, 'customerName', params.customerName);
  return request.get('/api/admin/vehicles', { params: backendParams });
}

export async function getVehicleDetail(id: number): Promise<VehicleDetail> {
  return request.get(`/api/admin/vehicles/${id}`);
}

export async function createVehicle(customerId: number, data: { frameNo: string; model?: string; batteryNo?: string; remark?: string }): Promise<number> {
  return request.post(`/api/admin/customers/${customerId}/vehicles`, data);
}

export async function updateVehicle(id: number, data: { frameNo: string; model?: string; batteryNo?: string; remark?: string }): Promise<void> {
  return request.put(`/api/admin/vehicles/${id}`, data);
}

export async function deleteVehicle(id: number): Promise<void> {
  return request.delete(`/api/admin/vehicles/${id}`);
}
