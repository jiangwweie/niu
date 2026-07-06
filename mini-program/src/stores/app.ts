export const appStore = {
  // Global app state can be put here
  systemInfo: null as any,
  
  init() {
    if (wx.getDeviceInfo && wx.getWindowInfo && wx.getAppBaseInfo) {
      this.systemInfo = {
        ...(wx.getSystemSetting ? wx.getSystemSetting() : {}),
        ...(wx.getAppAuthorizeSetting ? wx.getAppAuthorizeSetting() : {}),
        ...wx.getDeviceInfo(),
        ...wx.getWindowInfo(),
        ...wx.getAppBaseInfo()
      };
      return;
    }

    this.systemInfo = wx.getSystemInfoSync();
  }
};
