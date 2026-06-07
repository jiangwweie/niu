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
  { value: 'DRAFT', label: '新建中' },
  { value: 'REPAIRING', label: '维修中' },
  { value: 'REPAIR_DONE', label: '维修完成' },
  { value: 'DELIVERED', label: '已交付' },
  { value: 'CANCELLED', label: '已取消' },
];

function resolveProgressText(item: WorkOrder) {
  return getProgressStatusText(item.progressStatus || item.status, item.progressStatusText);
}

function resolveCashierText(item: WorkOrder) {
  const status = item.progressStatus || item.status;
  if (status === 'DRAFT' && item.cashierStatus === 'NO_CHARGE') {
    const hasItems = !!item.chargeItems?.length;
    return hasItems || Number(item.receivableAmount || 0) > 0 ? '草稿未提交' : '待录入费用';
  }
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
    hasCreateOrderPermission: false,
    hasUpdateOrderPermission: false,
    pageNo: 1,
    pageSize: 20,
  },
  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/work-orders/index') });
      return;
    }
    this.setData({
      hasCreateOrderPermission: hasPermission('WORK_ORDER_CREATE'),
      hasUpdateOrderPermission: hasPermission('WORK_ORDER_UPDATE')
    });
    this.fetchData();
  },
  goToCreate() {
    wx.navigateTo({ url: '/pages/create-work-order-placeholder/index' });
  },
  onSearch(e: any) {
    this.setData({ keyword: e.detail.value });
  },
  onSubmitSearch() {
    this.setData({ pageNo: 1 });
    this.fetchData();
  },
  onClear() {
    this.setData({ keyword: '', pageNo: 1 });
    this.fetchData();
  },
  onTabChange(e: any) {
    const status = e.currentTarget.dataset.status as string;
    this.setData({ activeStatus: status, pageNo: 1 });
    this.fetchData();
  },
  async fetchData() {
    try {
      const res = await getWorkOrders({
        keyword: this.data.keyword,
        status: this.data.activeStatus || undefined,
        pageNo: this.data.pageNo,
        pageSize: this.data.pageSize,
      });
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
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/work-order-detail/index?id=${id}`
    });
  },
  onEditDraft(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/create-work-order-placeholder/index?id=${id}`
    });
  }
});
