import { API_MODE, getBaseUrl } from './config';
import { ApiResponse } from '../types/common';
import { authStore } from '../stores/auth';
import { getFriendlyErrorMessage } from './statusText';

interface RequestOptions extends WechatMiniprogram.RequestOption {
  mockData?: any;
  showLoading?: boolean;
  /** 设为 true 时跳过 Authorization 注入，专门用于登录接口 */
  skipAuth?: boolean;
}

type ApiError = Error & {
  code?: string | number;
  statusCode?: number;
  traceId?: string;
};

export const request = <T = any>(options: RequestOptions): Promise<ApiResponse<T>> => {
  return new Promise((resolve, reject) => {
    if (options.showLoading) {
      wx.showLoading({ title: '加载中...', mask: true });
    }

    const complete = () => {
      if (options.showLoading) {
        wx.hideLoading();
      }
    };

    if (API_MODE === 'mock') {
      console.log(`[Mock API Request]: ${options.url}`, options);
      setTimeout(() => {
        complete();
        const res: ApiResponse<T> = {
          code: 'SUCCESS',
          message: 'success',
          data: options.mockData || ({} as T),
          traceId: `mock_trace_${Date.now()}`
        };
        resolve(res);
      }, 500);
      return;
    }

    authStore.getCurrentUser();
    const headers = {
      ...options.header,
      'Content-Type': 'application/json'
    } as Record<string, string>;

    if (!options.skipAuth && authStore.accessToken) {
      headers['Authorization'] = `Bearer ${authStore.accessToken}`;
    }

    wx.request({
      ...options,
      url: options.url.startsWith('http') ? options.url : `${getBaseUrl()}${options.url}`,
      header: headers,
      success: (res) => {
        complete();
        if (res.statusCode === 401) {
          authStore.clearAuth();
          const message = getFriendlyErrorMessage('UNAUTHORIZED');
          wx.showToast({ title: message, icon: 'none' });
          const currentPages = getCurrentPages();
          const route = currentPages.length ? currentPages[currentPages.length - 1].route : '';
          wx.redirectTo({ url: `/pages/login/index?redirect=${encodeURIComponent('/' + route)}` });
          reject(new Error(message));
          return;
        }
        if (res.statusCode === 403) {
          const apiRes = res.data as Partial<ApiResponse<any>> | undefined;
          const message = apiRes?.message || getFriendlyErrorMessage(apiRes?.code, getFriendlyErrorMessage('FORBIDDEN'));
          wx.showToast({ title: message, icon: 'none' });
          reject(new Error(message));
          return;
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          const apiRes = res.data as ApiResponse<T>;
          if (apiRes.code !== 'SUCCESS' && apiRes.code !== 200) {
            const message = apiRes.message || getFriendlyErrorMessage(apiRes.code);
            wx.showToast({ title: message, icon: 'none' });
            reject(new Error(message));
          } else {
            resolve(apiRes);
          }
        } else {
          const apiRes = res.data as Partial<ApiResponse<any>> | undefined;
          const message = apiRes?.message || getFriendlyErrorMessage(apiRes?.code, '请求失败，请稍后重试');
          const error = new Error(message) as ApiError;
          error.code = apiRes?.code;
          error.statusCode = res.statusCode;
          error.traceId = apiRes?.traceId;
          wx.showToast({ title: message, icon: 'none' });
          reject(error);
        }
      },
      fail: (err) => {
        complete();
        wx.showToast({ title: '网络请求失败，请检查网络连接', icon: 'none' });
        reject(err);
      }
    });
  });
};
