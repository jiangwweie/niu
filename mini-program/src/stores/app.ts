export const appStore = {
  // Global app state can be put here
  systemInfo: null as WechatMiniprogram.SystemInfo | null,
  
  init() {
    this.systemInfo = wx.getSystemInfoSync();
  }
};
