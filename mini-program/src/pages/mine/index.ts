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
