import { authApi } from '../../api/auth';
import { authStore } from '../../stores/auth';

Page({
  data: {
    username: '',
    password: '',
    captchaId: '',
    captchaText: '',
    captchaCode: '',
    captchaLoading: false,
    loading: false,
    wechatLoading: false
  },

  onLoad(options) {
    if (authStore.isLoggedIn) {
      wx.switchTab({ url: '/pages/dashboard/index' });
    }
    this.redirectUrl = options.redirect ? decodeURIComponent(options.redirect) : '';
    this.refreshCaptcha();
  },

  redirectUrl: '',

  onUsernameChange(e: any) {
    this.setData({ username: e.detail.value });
  },

  onPasswordChange(e: any) {
    this.setData({ password: e.detail.value });
  },

  onCaptchaChange(e: any) {
    this.setData({ captchaCode: e.detail.value });
  },

  async refreshCaptcha() {
    this.setData({ captchaLoading: true });
    try {
      const res = await authApi.getCaptcha();
      this.setData({
        captchaId: res.data.captchaId,
        captchaText: res.data.captchaText,
        captchaCode: ''
      });
    } catch (e) {
      wx.showToast({ title: '验证码加载失败', icon: 'none' });
    } finally {
      this.setData({ captchaLoading: false });
    }
  },

  async handleLogin() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入账号密码', icon: 'none' });
      return;
    }
    if (!this.data.captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      const res = await authApi.loginWithPassword({
        username: this.data.username,
        password: this.data.password,
        captchaId: this.data.captchaId,
        captchaCode: this.data.captchaCode
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
      this.refreshCaptcha();
    } finally {
      this.setData({ loading: false });
    }
  },

  async handleWechatLogin() {
    this.setData({ wechatLoading: true });
    try {
      const loginRes: WechatMiniprogram.LoginSuccessCallbackResult = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: reject
        });
      });

      if (!loginRes.code) {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
        return;
      }

      const res = await authApi.wechatLogin(loginRes.code);
      authStore.setToken(res.data.accessToken);
      authStore.setUser(res.data.user as any);

      wx.showToast({ title: '登录成功', icon: 'success' });

      setTimeout(() => {
        wx.switchTab({ url: '/pages/dashboard/index' });
      }, 500);
    } catch (e) {
      // authApi.wechatLogin handles toast for WECHAT_NOT_BOUND and other errors
    } finally {
      this.setData({ wechatLoading: false });
    }
  }
});
