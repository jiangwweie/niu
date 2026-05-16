import { authStore } from '../../stores/auth';
import { getInventoryStocks } from '../../api/inventory';
import { InventoryStock } from '../../types/inventory';
import { hasPermission } from '../../utils/permission';

type InventoryListItem = InventoryStock & {
  availableQtyClass: string;
  lastChangedAtText: string;
};

function pad(value: number) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(value?: string) {
  if (!value || value === '-') return '暂无';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '暂无';
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function getAvailableQtyClass(availableQty: number) {
  if (availableQty <= 0) return 'stock-danger';
  if (availableQty <= 2) return 'stock-warning';
  return 'stock-normal';
}

Page({
  data: {
    keyword: '',
    stocks: [] as InventoryListItem[],
    hasInboundPermission: false,
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/inventory/index') });
      return;
    }
    this.setData({
      hasInboundPermission: hasPermission('INVENTORY_INBOUND')
    });
    this.fetchData();
  },
  goToInbound() {
    wx.navigateTo({ url: '/pages/inbound-placeholder/index' });
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
      const stocks = (res.data.records || []).map((item: InventoryStock) => ({
        ...item,
        availableQtyClass: getAvailableQtyClass(Number(item.availableQty || 0)),
        lastChangedAtText: formatDateTime(item.lastChangedAt)
      }));
      this.setData({ stocks });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: `查看库存 ${id}`, icon: 'none' });
  }
});
