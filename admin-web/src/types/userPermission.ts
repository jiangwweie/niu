export interface SystemUser {
  id: string;
  userNo: string;
  name: string;
  phone: string;
  store: string;
  roleName: string;
  roleCode: string;
  enabled: boolean;
  lastLoginTime?: string;
  remark?: string;
}

export interface UserQuery {
  name?: string;
  phone?: string;
  roleCode?: string;
  enabled?: boolean | string;
  pageNo: number;
  pageSize: number;
}

export interface RoleInfo {
  id: string;
  roleCode: string;
  roleName: string;
  description: string;
  userCount: number;
  enabled: boolean;
}

export interface PermissionNode {
  id: string;
  permCode: string;
  permName: string;
  module: string;
  description: string;
  isCore: boolean;
}
