import request from '@/utils/request';
import type { StoreInfo, UpdateStoreRequest } from '@/types/store';

export async function getCurrentStore(): Promise<StoreInfo> {
  return await request.get('/api/admin/store/current');
}

export async function updateCurrentStore(data: UpdateStoreRequest): Promise<StoreInfo> {
  return await request.put('/api/admin/store/current', data);
}
