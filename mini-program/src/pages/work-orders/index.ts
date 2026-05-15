import { authStore } from '../../stores/auth';
import { getWorkOrders } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';

Page({
  data: {
    keyword: '',
    orders: [] as WorkOrder[],
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/work-orders/index') });
      return;
    }
    this.fetchData();
  },
  onSearch(e: any) {
    this.setData({ keyword: e.detail.value });
    this.fetchData();
  },
  onClear() {
    this.setData({ keyword: '' });
    this.fetchData();
  },
  async fetchData() {
    try {
      const res = await getWorkOrders({ keyword: this.data.keyword });
      this.setData({ orders: res.data.records });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/work-order-detail/index?id=${id}`
    });
  }
});
