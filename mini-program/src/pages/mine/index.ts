import { authStore } from '../../stores/auth';
import { mockUsers } from '../../mock/auth';
import { API_MODE } from '../../utils/config';
import { authApi } from '../../api/auth';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    currentUser: null,
    mockUsers: [],
    apiMode: API_MODE,
    hasReimbursementPermission: false
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
    this.setData({
      currentUser: authStore.getCurrentUser() as any,
      hasReimbursementPermission: hasPermission('REIMBURSEMENT_SUBMIT')
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
