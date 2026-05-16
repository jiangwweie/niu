import { authStore } from '../../stores/auth';
import { getWorkOrders } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';

const STATUS_TEXT_MAP: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_ACCEPT: '待接单',
  ACCEPTED: '已接单',
  PART_ORDERED: '已定件',
  PART_ARRIVED: '已到件',
  SETTLED: '已结算',
  CANCELLED: '已取消'
};

const STATUS_THEME_MAP: Record<string, string> = {
  DRAFT: 'default',
  PENDING_ACCEPT: 'warning',
  ACCEPTED: 'primary',
  PART_ORDERED: 'warning',
  PART_ARRIVED: 'success',
  SETTLED: 'success',
  CANCELLED: 'danger'
};

type WorkOrderListItem = WorkOrder & {
  statusText: string;
  statusTheme: string;
  createdAtText: string;
};

function pad(value: number) {
  return value < 10 ? `0${value}` : `${value}`;
}

function formatDateTime(value?: string) {
  if (!value) return '暂无';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '暂无';
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

Page({
  data: {
    keyword: '',
    orders: [] as WorkOrderListItem[],
    hasCreateOrderPermission: false,
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/work-orders/index') });
      return;
    }
    this.setData({
      hasCreateOrderPermission: hasPermission('WORK_ORDER_CREATE')
    });
    this.fetchData();
  },
  goToCreate() {
    wx.navigateTo({ url: '/pages/create-work-order-placeholder/index' });
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
      const orders = (res.data.records || []).map((item: WorkOrder) => ({
        ...item,
        statusText: STATUS_TEXT_MAP[item.status] || item.status || '未知',
        statusTheme: STATUS_THEME_MAP[item.status] || 'default',
        createdAtText: formatDateTime(item.createdAt)
      }));
      this.setData({ orders });
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
