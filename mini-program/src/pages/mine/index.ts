import { authStore } from '../../stores/auth';
import { mockUsers } from '../../mock/auth';
import { API_MODE } from '../../utils/config';
import { authApi } from '../../api/auth';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    currentUser: null,
    displayUserName: '',
    displayAccount: '',
    displayRole: '',
    displayStore: '',
    mockUsers: [],
    apiMode: API_MODE,
    hasReimbursementPermission: false,
    wechatBound: false,
    wechatBoundAt: '',
    wechatBindingLoading: false
  },
  onLoad() {
    this.setData({
      currentUser: authStore.getCurrentUser() as any,
      mockUsers: mockUsers as any
    });
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/mine/index') });
      return;
    }
    const user = authStore.getCurrentUser() as any;
    let name = user?.realName || user?.userName || user?.username || '未登录';
    try {
      name = decodeURIComponent(name);
    } catch(e) {}
    const account = user?.userName || user?.username || '';
    const role = user?.roleName || (Array.isArray(user?.roleCodes) ? user.roleCodes.join(' / ') : user?.roleCode || '');
    const store = user?.storeName || (user?.storeId != null ? `门店ID ${user.storeId}` : '');

    this.setData({
      currentUser: user,
      displayUserName: name,
      displayAccount: account,
      displayRole: role,
      displayStore: store,
      hasReimbursementPermission: hasPermission('REIMBURSEMENT_SUBMIT'),
      wechatBound: !!user?.wechatBound,
      wechatBoundAt: user?.wechatBoundAt || ''
    });
  },
  onSwitchUser(e: any) {
    if (this.data.apiMode === 'real') return;
    const userId = e.currentTarget.dataset.id;
    authStore.switchUser(userId);
    this.setData({
      currentUser: authStore.getCurrentUser() as any
    });
    wx.showToast({
      title: '切换成功',
      icon: 'success'
    });
  },

  goToReimbursement() {
    wx.navigateTo({
      url: '/pages/reimbursement-placeholder/index'
    });
  },

  async handleBindWechat() {
    this.setData({ wechatBindingLoading: true });
    try {
      const loginRes: WechatMiniprogram.LoginSuccessCallbackResult = await new Promise((resolve, reject) => {
        wx.login({ success: resolve, fail: reject });
      });

      if (!loginRes.code) {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
        return;
      }

      await authApi.bindWechat(loginRes.code);

      // 优先从服务端刷新用户信息（包含 wechatBound/wechatBoundAt）
      try {
        const meRes = await authApi.getMe();
        authStore.setUser(meRes.data as any);
        this.setData({
          currentUser: meRes.data as any,
          wechatBound: !!(meRes.data as any).wechatBound,
          wechatBoundAt: (meRes.data as any).wechatBoundAt || ''
        });
      } catch {
        // 兜底：/me 失败时本地更新
        const user = authStore.getCurrentUser() as any;
        if (user) {
          user.wechatBound = true;
          user.wechatBoundAt = new Date().toISOString();
          authStore.setUser(user);
          this.setData({
            currentUser: user,
            wechatBound: true,
            wechatBoundAt: user.wechatBoundAt
          });
        }
      }

      wx.showToast({ title: '绑定成功', icon: 'success' });
    } catch (e) {
      // request wrapper handles error toast
    } finally {
      this.setData({ wechatBindingLoading: false });
    }
  },

  async handleLogout() {
    try {
      await authApi.logout();
    } catch (e) {
      // ignore
    } finally {
      authStore.clearAuth();
      wx.showToast({ title: '已退出登录', icon: 'success' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/index' });
      }, 500);
    }
  }
})
