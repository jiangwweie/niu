import { getParts } from '../../api/parts';
import { Part } from '../../types/parts';

Page({
  data: {
    keyword: '',
    parts: [] as Part[],
  },
  onLoad() {
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
      const res = await getParts({ keyword: this.data.keyword });
      this.setData({ parts: res.data.records });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: `查看配件 ${id}`, icon: 'none' });
  }
});
