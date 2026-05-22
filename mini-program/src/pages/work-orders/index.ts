import { authStore } from '../../stores/auth';
import { getWorkOrders } from '../../api/workOrder';
import { WorkOrder } from '../../types/workOrder';
import { hasPermission } from '../../utils/permission';

const PROGRESS_TABS = [
  { value: '', label: '全部' },
  { value: 'REPAIRING', label: '维修中' },
  { value: 'REPAIR_DONE', label: '维修完成' },
  { value: 'DELIVERED', label: '已交付' },
  { value: 'CANCELLED', label: '已取消' },
];

const LEGACY_STATUSES = ['PENDING_ACCEPT', 'ACCEPTED', 'PART_ORDERED', 'PART_ARRIVED', 'SETTLED'];

function resolveProgressText(item: WorkOrder) {
  if (item.progressStatusText) return item.progressStatusText;
  if (item.progressStatus) {
    const map: Record<string, string> = {
      DRAFT: '新建中', REPAIRING: '维修中', REPAIR_DONE: '维修完成',
      DELIVERED: '已交付', CANCELLED: '已取消'
    };
    return map[item.progressStatus] || '未知';
  }
  if (item.status && LEGACY_STATUSES.indexOf(item.status) >= 0) return '旧状态，请先清理试运行数据';
  return '未知';
}

function resolveCashierText(item: WorkOrder) {
  if (item.cashierStatusText) return item.cashierStatusText;
  if (item.cashierStatus) {
    const map: Record<string, string> = {
      NO_CHARGE: '无需收款', UNPAID: '未收款', PARTIAL_PAID: '部分收款',
      PAID: '已收齐', REFUND_PENDING: '待退款', PARTIAL_REFUNDED: '部分退款', REFUNDED: '已退清'
    };
    return map[item.cashierStatus] || '';
  }
  return '';
}

function resolveInventoryText(item: WorkOrder) {
  if (item.inventoryStatusText) return item.inventoryStatusText;
  if (item.inventoryStatus) {
    const map: Record<string, string> = {
      NOT_RESERVED: '未预占', RESERVED: '已预占', CONSUMED: '已扣减', RELEASED: '已释放'
    };
    return map[item.inventoryStatus] || '';
  }
  return '';
}

function resolveProgressKey(item: WorkOrder) {
  if (item.progressStatus) return item.progressStatus;
  if (item.status && LEGACY_STATUSES.indexOf(item.status) >= 0) return 'LEGACY';
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
