import request from '@/utils/request';
import { setSearchParam } from '@/utils/searchParams';
import type { PaginatedResult } from '@/types';
import type {
  CreateUserRequest,
  CreateUserResponse,
  PermissionNode,
  ResetPasswordResponse,
  RoleInfo,
  SystemUser,
  UpdateUserRequest,
  UserQuery,
} from '@/types/userPermission';

export async function getUserList(params: UserQuery): Promise<PaginatedResult<SystemUser>> {
  const backendParams: Record<string, unknown> = {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
  setSearchParam(backendParams, 'username', params.username);
  setSearchParam(backendParams, 'realName', params.realName);
  setSearchParam(backendParams, 'phone', params.phone);
  setSearchParam(backendParams, 'roleCode', params.roleCode);
  if (params.storeId !== undefined && params.storeId !== '') {
    backendParams.storeId = params.storeId;
  }
  if (params.enabled !== undefined && params.enabled !== '') {
    backendParams.enabled = params.enabled;
  }
  return await request.get('/api/admin/users', { params: backendParams });
}

export async function getUserDetail(id: number): Promise<SystemUser> {
  return await request.get(`/api/admin/users/${id}`);
}

export async function createUser(data: CreateUserRequest): Promise<CreateUserResponse> {
  return await request.post('/api/admin/users', data);
}

export async function updateUser(id: number, data: UpdateUserRequest): Promise<SystemUser> {
  return await request.put(`/api/admin/users/${id}`, data);
}

export async function enableUser(id: number): Promise<void> {
  return await request.post(`/api/admin/users/${id}/enable`);
}

export async function disableUser(id: number): Promise<void> {
  return await request.post(`/api/admin/users/${id}/disable`);
}

export async function resetUserPassword(id: number): Promise<ResetPasswordResponse> {
  return await request.post(`/api/admin/users/${id}/reset-password`);
}

export async function unbindWechat(id: number): Promise<void> {
  return await request.post(`/api/admin/users/${id}/wechat/unbind`);
}

export async function getRoleList(): Promise<RoleInfo[]> {
  return await request.get('/api/admin/roles');
}

export async function getPermissionList(): Promise<PermissionNode[]> {
  return await request.get('/api/admin/permissions');
}
