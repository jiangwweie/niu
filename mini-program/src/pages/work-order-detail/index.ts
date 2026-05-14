import { getWorkOrderDetail } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';

Page({
  data: {
    orderId: '',
    order: null as WorkOrder | null,
  },
  onLoad(options: any) {
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
  }
});
