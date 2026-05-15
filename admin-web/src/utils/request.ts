import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';

const request = axios.create({
  timeout: 15000,
});

// Remove hardcoded X-User-Id and use JWT
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
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
    const msg = body?.message || '请求失败';
    ElMessage.error(msg);
    return Promise.reject(new Error(msg));
  },
  async (error: AxiosError<{ code?: string; message?: string }>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
      }
      return Promise.reject(new Error('未登录或登录已过期'));
    }
    if (error.response?.status === 403) {
      ElMessage.error('无权限访问该资源');
      return Promise.reject(new Error('无权限访问该资源'));
    }

    const data = error.response?.data as any;
    let msg = data?.message || error.message || '网络请求错误';
    if (data instanceof Blob && data.type.includes('application/json')) {
      try {
        const json = JSON.parse(await data.text());
        msg = json.message || msg;
      } catch {
        msg = '请求失败';
      }
    }
    ElMessage.error(msg);
    return Promise.reject(error);
  },
);

export default request;
