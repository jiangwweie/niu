import { authStore } from '../../stores/auth';
import { getInventoryStocks } from '../../api/inventory';
import { InventoryStock } from '../../types/inventory';

Page({
  data: {
    keyword: '',
    stocks: [] as InventoryStock[],
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/inventory/index') });
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
      const res = await getInventoryStocks({ keyword: this.data.keyword });
      this.setData({ stocks: res.data.records });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: `查看库存 ${id}`, icon: 'none' });
  }
});
