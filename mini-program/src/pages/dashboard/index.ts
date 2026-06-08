import { authStore } from '../../stores/auth';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    userName: '',
    greeting: '你好',
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

    const hour = new Date().getHours();
    let greeting = '你好';
    if (hour < 12) greeting = '早上好';
    else if (hour < 18) greeting = '下午好';
    else greeting = '晚上好';

    this.setData({
      userName: name,
      greeting,
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
  },
  // P0-01 fix: 工单提醒区入口跳转
  goToWorkOrders() {
    wx.switchTab({ url: '/pages/work-orders/index' });
  }
});
