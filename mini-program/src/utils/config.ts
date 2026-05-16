/**
 * 联调注意事项：
 * 1. 微信开发者工具中，可以在“详情 - 本地设置”中勾选“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”用于本地调试。
 * 2. 真机预览时，手机无法访问电脑的 localhost。
 * 3. 真机联调需要将 BASE_URL 配置为电脑的局域网 IP（且手机与电脑在同一局域网），或者使用内网穿透/测试环境 HTTPS 域名。
 */
export const API_MODE: 'mock' | 'real' = 'real'; // 接入本地后端
export const BASE_URL = 'http://localhost:8080'; // 可以修改为 http://局域网IP:8080 或 HTTPS 域名
