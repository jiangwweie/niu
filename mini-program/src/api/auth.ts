import { request } from '../utils/request';

export interface AuthUser {
  userId: number;
  storeId: number;
  username: string;
  realName?: string;
  roleCodes: string[];
  permissionCodes: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: AuthUser;
}

export const authApi = {
  loginWithPassword: (data: any) => request<LoginResponse>({
    url: '/api/auth/login/password',
    method: 'POST',
    data,
    showLoading: true
  }),

  getMe: () => request<AuthUser>({
    url: '/api/auth/me',
    method: 'GET'
  }),

  logout: () => request<void>({
    url: '/api/auth/logout',
    method: 'POST'
  })
};
