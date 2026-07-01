import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { getFriendlyErrorMessage } from './statusText';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
});

const AUTH_FREE_PATHS = ['/api/auth/login/password', '/api/auth/captcha'];
const GENERIC_SERVER_MESSAGES = new Set([
  'Unauthorized',
  'Forbidden',
  'Bad Request',
  'Internal Server Error',
  'Network Error',
  '未登录或登录已过期',
  '无权限访问该资源',
  '请求失败',
]);

type ApiErrorBody = {
  code?: string;
  message?: string;
};

export function isRequestErrorHandled(error: unknown): boolean {
  return Boolean((error as any)?.handledByInterceptor);
}

function getRequestPath(config: AxiosRequestConfig) {
  const url = config.url || '';
  try {
    const fullUrl = new URL(url, config.baseURL || window.location.origin);
    return fullUrl.pathname;
  } catch {
    return url.split('?')[0];
  }
}

function clearClientAuth() {
  localStorage.removeItem('accessToken');
  try {
    useAuthStore().clearAuth();
  } catch {
    // Pinia may not be active during early module initialization.
  }
}

function currentFullPath() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

function apiErrorMessage(data?: ApiErrorBody, fallback = '请求失败') {
  const message = data?.message;
  const normalizedFallback = message && !GENERIC_SERVER_MESSAGES.has(message)
    ? message
    : fallback;
  return getFriendlyErrorMessage(data?.code, normalizedFallback);
}

function markErrorHandled<T extends Error>(error: T, message: string): T {
  error.message = message;
  (error as any).handledByInterceptor = true;
  return error;
}

// Remove hardcoded X-User-Id and use JWT
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  const requestPath = getRequestPath(config);
  const shouldSkipAuth = AUTH_FREE_PATHS.includes(requestPath);
  if (token && !shouldSkipAuth) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Unwrap ApiResponse: { code, message, data, traceId } → return data on SUCCESS
request.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body instanceof Blob) {
      return response; // Return the full response for Blob to get headers
    }
    if (body && body.code === 'SUCCESS') {
      return body.data;
    }
    // Backend returned a business error inside 200
    const msg = apiErrorMessage(body);
    ElMessage.error(msg);
    const err = new Error(msg) as any;
    err.response = response;
    err.handledByInterceptor = true;
    return Promise.reject(err);
  },
  async (error: AxiosError<{ code?: string; message?: string }>) => {
    const requestPath = getRequestPath(error.config || {});
    const isAuthFreeRequest = AUTH_FREE_PATHS.includes(requestPath);
    if (error.response?.status === 401) {
      clearClientAuth();
      const data = error.response?.data as ApiErrorBody | undefined;
      const msg = requestPath === '/api/auth/login/password'
        ? '账号或密码不正确，或账号已停用'
        : apiErrorMessage(data, getFriendlyErrorMessage('UNAUTHORIZED'));
      markErrorHandled(error, msg);

      if (isAuthFreeRequest) {
        ElMessage.error(msg);
        return Promise.reject(error);
      }

      if (window.location.pathname !== '/login') {
        ElMessage.warning(getFriendlyErrorMessage('UNAUTHORIZED'));
        window.location.href = '/login?redirect=' + encodeURIComponent(currentFullPath());
      }
      return Promise.reject(error);
    }
    if (error.response?.status === 403) {
      const data = error.response?.data as ApiErrorBody | undefined;
      const msg = apiErrorMessage({ code: data?.code || 'FORBIDDEN', message: data?.message }, '当前账号无权操作。');
      ElMessage.error(msg);
      markErrorHandled(error, msg);
      return Promise.reject(error);
    }

    const data = error.response?.data as any;
    let msg = apiErrorMessage(data, error.message || '网络请求错误');
    if (data instanceof Blob && data.type.includes('application/json')) {
      try {
        const json = JSON.parse(await data.text());
        msg = apiErrorMessage(json, msg);
      } catch {
        msg = '请求失败';
      }
    }
    ElMessage.error(msg);
    markErrorHandled(error, msg);
    return Promise.reject(error);
  },
);

export default request;
