import request from '@/utils/request';

export interface PlatformStore {
  id: number;
  storeCode: string;
  storeName: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  status: string;
}

export interface CreateStoreRequest {
  storeName: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}

export interface UpdateStoreRequest {
  storeName?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
  status?: string;
}

export interface CreateStoreAdminRequest {
  username: string;
  realName?: string;
  phone?: string;
}

export interface CreateStoreAdminResponse {
  userId: number;
  username: string;
  temporaryPassword: string;
}

export function listPlatformStores(): Promise<PlatformStore[]> {
  return request.get('/api/platform/stores');
}

export function createPlatformStore(data: CreateStoreRequest): Promise<PlatformStore> {
  return request.post('/api/platform/stores', data);
}

export function updatePlatformStore(id: number, data: UpdateStoreRequest): Promise<PlatformStore> {
  return request.put(`/api/platform/stores/${id}`, data);
}

export function createStoreAdmin(storeId: number, data: CreateStoreAdminRequest): Promise<CreateStoreAdminResponse> {
  return request.post(`/api/platform/stores/${storeId}/admin-users`, data);
}
