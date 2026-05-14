import { API_MODE, BASE_URL } from './config';
import { ApiResponse } from '../types/common';
import { authStore } from '../stores/auth';

interface RequestOptions extends WechatMiniprogram.RequestOption {
  mockData?: any;
  showLoading?: boolean;
}

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

    const user = authStore.getCurrentUser();
    const headers = {
      ...options.header,
      'Content-Type': 'application/json'
    } as Record<string, string>;

    if (user) {
      headers['X-User-Id'] = user.userId;
      headers['X-Store-Id'] = user.storeId;
    }

    wx.request({
      ...options,
      url: options.url.startsWith('http') ? options.url : `${BASE_URL}${options.url}`,
      header: headers,
      success: (res) => {
        complete();
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const apiRes = res.data as ApiResponse<T>;
          // check if 'SUCCESS' or 200 (in case some api return 200 int)
          if (apiRes.code !== 'SUCCESS' && apiRes.code !== 200) {
            wx.showToast({ title: apiRes.message || '请求失败', icon: 'none' });
            reject(new Error(apiRes.message || 'API Error'));
          } else {
            resolve(apiRes);
          }
        } else {
          wx.showToast({ title: '网络请求失败，请检查后端服务或开发环境配置', icon: 'none' });
          reject(new Error(`HTTP Error: ${res.statusCode}`));
        }
      },
      fail: (err) => {
        complete();
        wx.showToast({ title: '网络请求失败，请检查后端服务或开发环境配置', icon: 'none' });
        reject(err);
      }
    });
  });
};
