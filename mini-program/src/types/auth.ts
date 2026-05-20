export interface User {
  userId: string;
  userName: string;
  roleCode: string;
  roleName: string;
  storeId: string;
  storeName: string;
  permissionCodes: string[];
  wechatBound?: boolean;
  wechatBoundAt?: string;
}
