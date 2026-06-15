import { getWorkOrders } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';
import { requireAnyPermission, requireLogin } from '../../utils/permission';

type CashierOrder = WorkOrder & {
  customerNameText: string;
  statusText: string;
  receivableText: string;
  outstandingText: string;
  refundableText: string;
};

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function buildCashierOrder(order: WorkOrder): CashierOrder {
  const outstanding = order.outstandingAmount != null
    ? order.outstandingAmount
    : Math.max(Number(order.receivableAmount || 0) - Number(order.receivedAmount || 0), 0);
  return {
    ...order,
    customerNameText: order.customerNameSnapshot || '未登记客户',
    statusText: order.progressStatusText || order.status || '-',
    receivableText: money(order.receivableAmount),
    outstandingText: money(outstanding),
    refundableText: money(order.refundableAmount || 0)
  };
}

Page({
  data: {
    orders: [] as CashierOrder[],
    loading: false
  },

  onShow() {
    if (!requireLogin('/pages/cashier-workbench/index')) return;
    if (!requireAnyPermission(['PAYMENT_RECORD', 'REFUND_RECORD', 'WORK_ORDER_VIEW'], '当前账号无权访问收银工作台')) return;
    this.fetchData();
  },

  async fetchData() {
    this.setData({ loading: true });
    try {
      const res = await getWorkOrders({ pageNo: 1, pageSize: 50 });
      const orders = (res.data.records || [])
        .map(buildCashierOrder)
        .filter(order => Number(order.outstandingAmount || 0) > 0 || Number(order.refundableAmount || 0) > 0);
      this.setData({ orders, loading: false });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '收银工单加载失败', icon: 'none' });
    }
  },

  onTapOrder(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/work-order-detail/index?id=${id}` });
  }
});
