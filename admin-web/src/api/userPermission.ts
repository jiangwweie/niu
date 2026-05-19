import request from '@/utils/request';
import type { PaginatedResult } from '@/types';
import type {
  CreateUserRequest,
  PermissionNode,
  ResetPasswordRequest,
  ResetPasswordResponse,
  RoleInfo,
  SystemUser,
  UpdateUserRequest,
  UserQuery,
} from '@/types/userPermission';

export async function getUserList(params: UserQuery): Promise<PaginatedResult<SystemUser>> {
  return await request.get('/api/admin/users', { params });
}

export async function getUserDetail(id: number): Promise<SystemUser> {
  return await request.get(`/api/admin/users/${id}`);
}

export async function createUser(data: CreateUserRequest): Promise<SystemUser> {
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

export async function resetUserPassword(id: number, data: ResetPasswordRequest): Promise<ResetPasswordResponse> {
  return await request.post(`/api/admin/users/${id}/reset-password`, data);
}

export async function getRoleList(): Promise<RoleInfo[]> {
  return await request.get('/api/admin/roles');
}

export async function getPermissionList(): Promise<PermissionNode[]> {
  return await request.get('/api/admin/permissions');
}
