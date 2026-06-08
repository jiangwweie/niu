import { getParts } from '../../api/parts';
import { Part } from '../../types/parts';
import { authStore } from '../../stores/auth';

let partSearchTimer: number | undefined;

Page({
  data: {
    keyword: '',
    parts: [] as Part[],
    loading: false,
  },
  onLoad() {
    this.fetchData();
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/parts/index') });
      return;
    }
  },
  onSearch(e: any) {
    this.setData({ keyword: e.detail.value });
    if (partSearchTimer) {
      clearTimeout(partSearchTimer);
    }
    partSearchTimer = setTimeout(() => {
      this.fetchData();
    }, 300) as unknown as number;
  },
  onClear() {
    this.setData({ keyword: '' });
    this.fetchData();
  },
  async fetchData() {
    this.setData({ loading: true });
    try {
      const res = await getParts({ keyword: this.data.keyword });
      this.setData({ parts: res.data.records, loading: false });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '网络异常，请稍后重试', icon: 'none', duration: 2000 });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: `查看配件 ${id}`, icon: 'none' });
  }
});
