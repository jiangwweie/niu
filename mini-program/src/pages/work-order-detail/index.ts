import { getWorkOrderDetail } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';

Page({
  data: {
    orderId: '',
    order: null as WorkOrder | null,
    hasCancelPermission: false,
    hasSettlePermission: false,
    hasPaymentPermission: false,
    hasRefundPermission: false,
  },
  onLoad(options: any) {
    this.setData({
      hasCancelPermission: hasPermission('WORK_ORDER_CANCEL'),
      hasSettlePermission: hasPermission('WORK_ORDER_SETTLE'),
      hasPaymentPermission: hasPermission('PAYMENT_RECORD'),
      hasRefundPermission: hasPermission('REFUND_RECORD')
    });
    if (options.id) {
      this.setData({ orderId: options.id });
      this.fetchData();
    }
  },
  async fetchData() {
    try {
      const res = await getWorkOrderDetail(this.data.orderId);
      this.setData({ order: res.data });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onCancel() {
    wx.showToast({ title: '取消工单功能开发中', icon: 'none' });
  },
  onSettle() {
    wx.showToast({ title: '结算功能开发中', icon: 'none' });
  },
  onPayment() {
    wx.showToast({ title: '记录支付功能开发中', icon: 'none' });
  },
  onRefund() {
    wx.showToast({ title: '记录退款功能开发中', icon: 'none' });
  }
});
