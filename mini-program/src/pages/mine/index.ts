import { authStore } from '../../stores/auth';
import { mockUsers } from '../../mock/auth';

Page({
  data: {
    currentUser: null,
    mockUsers: []
  },
  onLoad() {
    this.setData({
      currentUser: authStore.getCurrentUser() as any,
      mockUsers: mockUsers as any
    });
  },
  onShow() {
    this.setData({
      currentUser: authStore.getCurrentUser() as any
    });
  },
  onSwitchUser(e: any) {
    const userId = e.currentTarget.dataset.id;
    authStore.switchUser(userId);
    this.setData({
      currentUser: authStore.getCurrentUser() as any
    });
    wx.showToast({
      title: '切换成功',
      icon: 'success'
    });
  }
})
