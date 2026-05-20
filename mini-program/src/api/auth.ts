import { request } from '../utils/request';
import { BASE_URL } from '../utils/config';
import { authStore } from '../stores/auth';
import type { ApiResponse } from '../types/common';

export interface AuthUser {
  userId: number;
  storeId: number;
  username: string;
  realName?: string;
  roleCodes: string[];
  permissionCodes: string[];
  wechatBound?: boolean;
  wechatBoundAt?: string;
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
    showLoading: true,
    skipAuth: true  // 登录接口不注入旧 token，避免过期 token 导致 401
  }),

  /**
   * 微信快捷登录 — 使用 raw wx.request 以便区分 401 的具体错误码，
   * 避免通用 request 包装器对 401 的默认拦截（清空 token + 跳转登录页）。
   */
  wechatLogin: (code: string): Promise<ApiResponse<LoginResponse>> => {
    return new Promise((resolve, reject) => {
      wx.showLoading({ title: '登录中...', mask: true });
      wx.request({
        url: `${BASE_URL}/api/auth/login/wechat`,
        method: 'POST',
        data: { code },
        header: { 'Content-Type': 'application/json' },
        success: (res) => {
          wx.hideLoading();
          const body = res.data as ApiResponse<LoginResponse>;
          if (res.statusCode === 200 && body.code === 'SUCCESS') {
            resolve(body);
          } else if (res.statusCode === 401 && body.code === 'WECHAT_NOT_BOUND') {
            wx.showToast({ title: '请先使用账号密码登录并绑定微信', icon: 'none', duration: 3000 });
            reject(new Error(String(body.code)));
          } else {
            wx.showToast({ title: body.message || '微信登录失败', icon: 'none' });
            reject(new Error(String(body.code) || 'WECHAT_LOGIN_FAILED'));
          }
        },
        fail: () => {
          wx.hideLoading();
          wx.showToast({ title: '网络请求失败', icon: 'none' });
          reject(new Error('NETWORK_ERROR'));
        }
      });
    });
  },

  bindWechat: (code: string) => request<void>({
    url: '/api/auth/wechat/bind',
    method: 'POST',
    data: { code }
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
