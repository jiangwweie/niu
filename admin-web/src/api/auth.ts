import request from '@/utils/request';
import type { AuthUser } from '@/stores/auth';

export interface LoginRequest {
  username: string;
  password?: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: AuthUser;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export async function loginWithPassword(data: LoginRequest): Promise<LoginResponse> {
  return await request.post('/api/auth/login/password', data);
}

export async function getMe(): Promise<AuthUser> {
  return await request.get('/api/auth/me');
}

export async function logout(): Promise<void> {
  return await request.post('/api/auth/logout');
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  return await request.post('/api/auth/change-password', data);
}
