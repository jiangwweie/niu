import { API_MODE, getBaseUrl } from './config';
import { ApiResponse } from '../types/common';
import { authStore } from '../stores/auth';
import { getFriendlyErrorMessage } from './statusText';

interface RequestOptions extends WechatMiniprogram.RequestOption {
  mockData?: any;
  showLoading?: boolean;
  /** 设为 true 时跳过 Authorization 注入，专门用于登录接口 */
  skipAuth?: boolean;
  /** 设为 true 时不由通用请求层弹出错误提示，交给页面自行处理 */
  suppressErrorToast?: boolean;
}

type ApiError = Error & {
  code?: string | number;
  statusCode?: number;
  traceId?: string;
  feedbackShown?: boolean;
};

function createApiError(message: string, params: Partial<ApiError> = {}): ApiError {
  const error = new Error(message) as ApiError;
  error.code = params.code;
  error.statusCode = params.statusCode;
  error.traceId = params.traceId;
  error.feedbackShown = params.feedbackShown;
  return error;
}

function showFeedback(error: ApiError, options: RequestOptions) {
  if (options.suppressErrorToast) return;

  const code = String(error.code || '');
  if (error.statusCode === 401 && !options.skipAuth) {
    wx.showModal({
      title: '需要重新登录',
      content: error.message,
      showCancel: false,
      confirmText: '去登录',
      success: () => {
        const currentPages = getCurrentPages();
        const route = currentPages.length ? currentPages[currentPages.length - 1].route : '';
        wx.redirectTo({ url: `/pages/login/index?redirect=${encodeURIComponent('/' + route)}` });
      }
    });
    error.feedbackShown = true;
    return;
  }

  if (error.statusCode === 403 || code === 'PASSWORD_CHANGE_REQUIRED') {
    wx.showModal({
      title: code === 'PASSWORD_CHANGE_REQUIRED' ? '需要修改密码' : '无权限访问',
      content: error.message,
      showCancel: false,
      confirmText: '我知道了'
    });
    error.feedbackShown = true;
    return;
  }

  wx.showToast({
    title: error.message,
    icon: 'none',
    duration: 3000
  });
  error.feedbackShown = true;
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
        const apiRes = res.data as Partial<ApiResponse<any>> | undefined;

        if (res.statusCode === 401) {
          if (!options.skipAuth) {
            authStore.clearAuth();
          }
          const message = options.skipAuth
            ? getFriendlyErrorMessage('LOGIN_BAD_CREDENTIALS')
            : getFriendlyErrorMessage(apiRes?.code, apiRes?.message);
          const error = createApiError(message, {
            code: apiRes?.code || 'UNAUTHORIZED',
            statusCode: res.statusCode,
            traceId: apiRes?.traceId
          });
          showFeedback(error, options);
          reject(error);
          return;
        }

        if (res.statusCode === 403) {
          const message = getFriendlyErrorMessage(apiRes?.code, apiRes?.message || getFriendlyErrorMessage('FORBIDDEN'));
          const error = createApiError(message, {
            code: apiRes?.code || 'FORBIDDEN',
            statusCode: res.statusCode,
            traceId: apiRes?.traceId
          });
          showFeedback(error, options);
          reject(error);
          return;
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          const typedApiRes = res.data as ApiResponse<T>;
          if (typedApiRes.code !== 'SUCCESS' && typedApiRes.code !== 200) {
            const message = getFriendlyErrorMessage(typedApiRes.code, typedApiRes.message);
            const error = createApiError(message, {
              code: typedApiRes.code,
              statusCode: res.statusCode,
              traceId: typedApiRes.traceId
            });
            showFeedback(error, options);
            reject(error);
          } else {
            resolve(typedApiRes);
          }
        } else {
          const message = apiRes?.message || getFriendlyErrorMessage(apiRes?.code, '请求失败，请稍后重试');
          const error = createApiError(getFriendlyErrorMessage(apiRes?.code, message), {
            code: apiRes?.code,
            statusCode: res.statusCode,
            traceId: apiRes?.traceId
          });
          showFeedback(error, options);
          reject(error);
        }
      },
      fail: (err) => {
        complete();
        const error = createApiError('网络连接失败，请检查网络后重试。', {
          code: 'NETWORK_ERROR'
        });
        showFeedback(error, options);
        reject(error);
      }
    });
  });
};
