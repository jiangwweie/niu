export interface StoreInfo {
  id: number;
  storeCode: string;
  storeName: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}

export interface UpdateStoreRequest {
  storeName: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}
