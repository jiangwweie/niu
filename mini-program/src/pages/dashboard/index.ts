import { authStore } from '../../stores/auth';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    userName: '',
    greeting: '你好',
    hasCreateOrderPermission: false,
    hasWorkOrderPermission: false,
    hasInboundPermission: false,
    hasInventoryPermission: false,
    hasCustomerManagePermission: false,
    hasReimbursementPermission: false,
    hasFundingPermission: false,
    hasAnyQuickAction: false
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

    const hasCreateOrderPermission = hasPermission('WORK_ORDER_CREATE');
    const hasWorkOrderPermission = [
      'WORK_ORDER_VIEW',
      'WORK_ORDER_CREATE',
      'WORK_ORDER_UPDATE',
      'WORK_ORDER_SETTLE'
    ].some(code => hasPermission(code));
    const hasInboundPermission = hasPermission('INVENTORY_INBOUND');
    const hasInventoryPermission = [
      'INVENTORY_VIEW',
      'INVENTORY_INBOUND',
      'INVENTORY_ADJUST'
    ].some(code => hasPermission(code));
    const hasCustomerManagePermission = hasPermission('CUSTOMER_MANAGE');
    const hasReimbursementPermission = hasPermission('REIMBURSEMENT_SUBMIT');
    const hasFundingPermission = [
      'FUNDING_APPLICATION_VIEW',
      'FUNDING_APPLICATION_MANAGE',
      'FUNDING_LEDGER_VIEW',
      'FUNDING_LEDGER_MANAGE',
      'FUNDING_PAYMENT_RECORD'
    ].some(code => hasPermission(code));

    this.setData({
      userName: name,
      greeting,
      hasCreateOrderPermission,
      hasWorkOrderPermission,
      hasInboundPermission,
      hasInventoryPermission,
      hasCustomerManagePermission,
      hasReimbursementPermission,
      hasFundingPermission,
      hasAnyQuickAction: hasCreateOrderPermission || hasInboundPermission || hasInventoryPermission
        || hasCustomerManagePermission || hasReimbursementPermission || hasFundingPermission
    });
  },
  goToCreateOrder() {
    wx.navigateTo({ url: '/pages/create-work-order/index' });
  },
  goToCustomerCreate() {
    wx.navigateTo({ url: '/pages/customer-create/index' });
  },
  goToInbound() {
    wx.navigateTo({ url: '/pages/inbound/index' });
  },
  goToReimbursement() {
    wx.navigateTo({ url: '/pages/reimbursement/index' });
  },
  goToFunding() {
    wx.navigateTo({ url: '/pages/funding/index' });
  },
  goToInventory() {
    wx.switchTab({ url: '/pages/inventory/index' });
  },
  // P0-01 fix: 工单提醒区入口跳转
  goToWorkOrders() {
    wx.switchTab({ url: '/pages/work-orders/index' });
  }
});
