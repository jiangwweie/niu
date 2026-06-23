/**
 * 联调注意事项：
 * 1. 微信开发者工具中，可以在“详情 - 本地设置”中勾选“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”用于本地调试。
 * 2. 真机预览时，手机无法访问电脑的 localhost。
 * 3. 真机联调需要将 DEV_BASE_URL 配置为电脑的局域网 IP（且手机与电脑在同一局域网），或者使用内网穿透/测试环境 HTTPS 域名。
 */
export const API_MODE: 'mock' | 'real' = 'real'; // 接入本地后端

export type ApiEnv = 'dev' | 'prod';
export const API_ENV: ApiEnv = 'prod';

export const DEV_BASE_URL = 'http://192.168.123.243:18080';
export const PROD_BASE_URL = 'https://api.qytech.online';

const BASE_URLS: Record<ApiEnv, string> = {
  dev: DEV_BASE_URL,
  prod: PROD_BASE_URL,
};

export const BASE_URL = BASE_URLS[API_ENV];

export function getBaseUrl() {
  try {
    const override = wx.getStorageSync('apiBaseUrl');
    if (typeof override === 'string' && /^https?:\/\//.test(override.trim())) {
      return override.trim().replace(/\/+$/, '');
    }
  } catch (e) {}
  return BASE_URL;
}
