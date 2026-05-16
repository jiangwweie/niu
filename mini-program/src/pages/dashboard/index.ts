import { authStore } from '../../stores/auth';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    userName: '',
    hasCreateOrderPermission: false,
    hasInboundPermission: false,
    hasReimbursementPermission: false
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/dashboard/index') });
      return;
    }
    
    const user = authStore.getCurrentUser() as any;
    let name = user?.realName || user?.userName || user?.username || '员工';
    try {
      name = decodeURIComponent(name);
    } catch (e) {}

    this.setData({
      userName: name,
      hasCreateOrderPermission: hasPermission('WORK_ORDER_CREATE'),
      hasInboundPermission: hasPermission('INVENTORY_INBOUND'),
      hasReimbursementPermission: hasPermission('REIMBURSEMENT_SUBMIT')
    });
  },
  goToCreateOrder() {
    wx.navigateTo({ url: '/pages/create-work-order-placeholder/index' });
  },
  goToInbound() {
    wx.navigateTo({ url: '/pages/inbound-placeholder/index' });
  },
  goToReimbursement() {
    wx.navigateTo({ url: '/pages/reimbursement-placeholder/index' });
  },
  goToInventory() {
    wx.switchTab({ url: '/pages/inventory/index' });
  }
});
