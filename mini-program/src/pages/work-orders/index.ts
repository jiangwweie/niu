import { authStore } from '../../stores/auth';
import { getWorkOrders } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';
import {
  getCashierStatusText,
  getInventoryStatusText,
  getProgressStatusText,
  isLegacyWorkOrderStatus,
} from '../../utils/statusText';

const PROGRESS_TABS = [
  { value: '', label: '全部' },
  { value: 'REPAIRING', label: '维修中' },
  { value: 'REPAIR_DONE', label: '维修完成' },
  { value: 'DELIVERED', label: '已交付' },
  { value: 'CANCELLED', label: '已取消' },
];

function resolveProgressText(item: WorkOrder) {
  return getProgressStatusText(item.progressStatus || item.status, item.progressStatusText);
}

function resolveCashierText(item: WorkOrder) {
  return getCashierStatusText(item.cashierStatus, item.cashierStatusText);
}

function resolveInventoryText(item: WorkOrder) {
  return getInventoryStatusText(item.inventoryStatus, item.inventoryStatusText);
}

function resolveProgressKey(item: WorkOrder) {
  if (item.progressStatus) return item.progressStatus;
  if (isLegacyWorkOrderStatus(item.status)) return 'LEGACY';
  return 'UNKNOWN';
}

function formatDateTime(value?: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

type WorkOrderListItem = WorkOrder & {
  progressText: string;
  cashierText: string;
  inventoryText: string;
  progressKey: string;
  createdAtText: string;
  outstandingDisplay: string;
};

Page({
  data: {
    keyword: '',
    activeStatus: '',
    progressTabs: PROGRESS_TABS,
    orders: [] as WorkOrderListItem[],
    filteredOrders: [] as WorkOrderListItem[],
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
  onTabChange(e: any) {
    const status = e.currentTarget.dataset.status as string;
    this.setData({ activeStatus: status });
    this.applyFilter();
  },
  applyFilter() {
    const { orders, activeStatus } = this.data;
    const filtered = activeStatus
      ? orders.filter(o => o.progressKey === activeStatus)
      : orders;
    this.setData({ filteredOrders: filtered });
  },
  async fetchData() {
    try {
      const res = await getWorkOrders({ keyword: this.data.keyword });
      const orders = (res.data.records || []).map((item: WorkOrder) => ({
        ...item,
        progressText: resolveProgressText(item),
        cashierText: resolveCashierText(item),
        inventoryText: resolveInventoryText(item),
        progressKey: resolveProgressKey(item),
        createdAtText: formatDateTime(item.createdAt),
        outstandingDisplay: (item.outstandingAmount != null ? item.outstandingAmount : Math.max(0, (item.receivableAmount || 0) - (item.receivedAmount || 0))).toFixed(2),
      }));
      this.setData({ orders });
      this.applyFilter();
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
