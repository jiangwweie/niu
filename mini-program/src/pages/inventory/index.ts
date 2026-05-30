import { authStore } from '../../stores/auth';
import { getInventoryStocks } from '../../api/inventory';
import { InventoryStock } from '../../types/inventory';
import { hasPermission } from '../../utils/permission';

const SOURCE_TEXT_MAP: Record<string, string> = {
  OFFICIAL: '官方',
  THIRD_PARTY: '第三方',
};

type InventoryListItem = InventoryStock & {
  availableQtyClass: string;
  lastChangedAtText: string;
  sourceText: string;
};

function pad(value: number) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(value?: string) {
  if (!value || value === '-') return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function getAvailableQtyClass(availableQty: number) {
  if (availableQty <= 0) return 'stock-danger';
  if (availableQty <= 3) return 'stock-warning';
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
      const stocks = (res.data.records || []).map((item: InventoryStock) => {
        const avail = Number(item.availableQty || 0);
        // 兼容 partSource / source 两种字段名
        const sourceKey = (item as any).partSource || (item as any).source || '';
        return {
          ...item,
          availableQtyClass: getAvailableQtyClass(avail),
          lastChangedAtText: formatDateTime(item.lastChangedAt),
          sourceText: SOURCE_TEXT_MAP[sourceKey] || '',
          partSource: sourceKey,
        };
      });
      this.setData({ stocks });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: `配件 ID: ${id}`, icon: 'none' });
  }
});
