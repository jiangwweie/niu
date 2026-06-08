export interface SystemUser {
  id: number;
  storeId: number;
  username: string;
  realName: string;
  phone?: string;
  roleCodes: string[];
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
  pageNo: number;
  pageSize: number;
}

export interface CreateUserRequest {
  username: string;
  realName: string;
  phone?: string;
  roleCodes: string[];
  initialPassword?: string;
  enabled?: boolean;
}

export interface UpdateUserRequest {
  realName?: string;
  phone?: string;
  roleCodes?: string[];
  enabled?: boolean;
}

export interface ResetPasswordRequest {
  temporaryPassword?: string;
}

export interface ResetPasswordResponse {
  temporaryPassword: string;
}

export interface RoleInfo {
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
