import { authApi } from '../../api/auth';
import { authStore } from '../../stores/auth';

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onLoad(options) {
    if (authStore.isLoggedIn) {
      wx.switchTab({ url: '/pages/dashboard/index' });
    }
    this.redirectUrl = options.redirect ? decodeURIComponent(options.redirect) : '';
  },

  redirectUrl: '',

  onUsernameChange(e: any) {
    this.setData({ username: e.detail.value });
  },

  onPasswordChange(e: any) {
    this.setData({ password: e.detail.value });
  },

  async handleLogin() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入账号密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      const res = await authApi.loginWithPassword({
        username: this.data.username,
        password: this.data.password
      });
      authStore.setToken(res.data.accessToken);
      authStore.setUser(res.data.user as any);
      
      wx.showToast({ title: '登录成功', icon: 'success' });
      
      setTimeout(() => {
        if (this.redirectUrl && this.redirectUrl.startsWith('/pages/')) {
          const tabPages = ['/pages/dashboard/index', '/pages/work-orders/index', '/pages/inventory/index', '/pages/mine/index'];
          if (tabPages.includes(this.redirectUrl)) {
            wx.switchTab({ url: this.redirectUrl });
          } else {
            wx.redirectTo({ url: this.redirectUrl });
          }
        } else {
          wx.switchTab({ url: '/pages/dashboard/index' });
        }
      }, 500);
    } catch (e) {
      // request wrapper handles error toast
    } finally {
      this.setData({ loading: false });
    }
  }
});
