import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { getFriendlyErrorMessage } from './statusText';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
});

const AUTH_FREE_PATHS = ['/api/auth/login/password', '/api/auth/captcha'];

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
    const msg = body?.message || getFriendlyErrorMessage(body?.code);
    ElMessage.error(msg);
    const err = new Error(msg) as any;
    err.response = response;
    return Promise.reject(err);
  },
  async (error: AxiosError<{ code?: string; message?: string }>) => {
    const requestPath = getRequestPath(error.config || {});
    const isAuthFreeRequest = AUTH_FREE_PATHS.includes(requestPath);
    if (error.response?.status === 401) {
      clearClientAuth();
      const data = error.response?.data as any;
      const msg = requestPath === '/api/auth/login/password'
        ? '账号或密码不正确，或账号已停用'
        : (data?.message || getFriendlyErrorMessage('UNAUTHORIZED'));
      error.message = msg;

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
      const data = error.response?.data as any;
      const msg = data?.message || getFriendlyErrorMessage(data?.code || 'FORBIDDEN');
      ElMessage.error(msg);
      error.message = msg;
      return Promise.reject(error);
    }

    const data = error.response?.data as any;
    let msg = data?.message || getFriendlyErrorMessage(data?.code, error.message || '网络请求错误');
    if (data instanceof Blob && data.type.includes('application/json')) {
      try {
        const json = JSON.parse(await data.text());
        msg = json.message || getFriendlyErrorMessage(json.code, msg);
      } catch {
        msg = '请求失败';
      }
    }
    ElMessage.error(msg);
    error.message = msg;
    return Promise.reject(error);
  },
);

export default request;
