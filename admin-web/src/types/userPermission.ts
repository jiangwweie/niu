export interface UserRoleInfo {
  roleId: number;
  storeId?: number | null;
  storeName?: string | null;
  roleCode: string;
  roleName: string;
}

export interface SystemUser {
  id: number;
  storeId?: number | null;
  storeName?: string | null;
  accountType?: string;
  username: string;
  realName: string;
  phone?: string;
  roleCodes: string[];
  roles?: UserRoleInfo[];
  enabled: boolean;
  passwordMustChange: boolean;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
  permissionCodes?: string[];
  passwordChangedAt?: string;
  wechatBound?: boolean;
  wechatBoundAt?: string;
}

export interface UserQuery {
  username?: string;
  realName?: string;
  phone?: string;
  roleCode?: string;
  enabled?: boolean | string;
  storeId?: number | string;
  pageNo: number;
  pageSize: number;
}

export interface CreateUserRequest {
  username: string;
  realName: string;
  phone?: string;
  storeId?: number | null;
  remark?: string;
  roleIds: number[];
  enabled?: boolean;
}

export interface CreateUserResponse {
  user: SystemUser;
  temporaryPassword: string;
}

export interface UpdateUserRequest {
  realName?: string;
  phone?: string;
  remark?: string;
  roleIds?: number[];
  enabled?: boolean;
}

export interface ResetPasswordResponse {
  temporaryPassword: string;
}

export interface RoleInfo {
  roleId: number;
  storeId?: number | null;
  storeName?: string | null;
  roleCode: string;
  roleName: string;
  description?: string;
  permissionCodes: string[];
}

export interface PermissionNode {
  permissionCode: string;
  permissionName: string;
  module: string;
}
