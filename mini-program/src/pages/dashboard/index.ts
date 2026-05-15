import { authStore } from '../../stores/auth';

Page({
  data: {},
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/dashboard/index') });
    }
  }
});
