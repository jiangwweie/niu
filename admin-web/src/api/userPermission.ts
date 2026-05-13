import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { SystemUser, UserQuery, RoleInfo, PermissionNode } from '@/types/userPermission';
import { mockUsers, mockRoles, mockPermissions } from '@/mock/userPermission';

export const getUserList = (params: UserQuery): Promise<BaseHttpResponse<PaginatedResult<SystemUser>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockUsers];
      
      if (params.name) filtered = filtered.filter(p => p.name.includes(params.name!));
      if (params.phone) filtered = filtered.filter(p => p.phone.includes(params.phone!));
      if (params.roleCode) filtered = filtered.filter(p => p.roleCode === params.roleCode);
      if (params.enabled !== undefined && params.enabled !== '') {
        const isEnabled = params.enabled === true || params.enabled === 'true';
        filtered = filtered.filter(p => p.enabled === isEnabled);
      }
      
      const page = params.pageNo || 1;
      const pageSize = params.pageSize || 10;
      const start = (page - 1) * pageSize;
      const pagedData = filtered.slice(start, start + pageSize);

      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: {
          records: pagedData,
          total: filtered.length,
          page,
          pageSize
        }
      });
    }, 300);
  });
};

export const getRoleList = (): Promise<BaseHttpResponse<RoleInfo[]>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: [...mockRoles]
      });
    }, 300);
  });
};

export const getPermissionList = (): Promise<BaseHttpResponse<PermissionNode[]>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: [...mockPermissions]
      });
    }, 300);
  });
};
