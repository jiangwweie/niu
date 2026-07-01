import { getPartDetail } from '../../api/parts';
import { Part } from '../../types/parts';
import { requireAnyPermission, requireLogin } from '../../utils/permission';
import { showRequestErrorToast } from '../../utils/requestError';

const PART_PAGE_PERMISSIONS = ['PART_VIEW', 'PART_MANAGE', 'PART_CREATE'];

type PartDetailView = Part & {
  sourceText: string;
  officialPartNoLabel: string;
  costPriceText: string;
  salePriceText: string;
  statusText: string;
};

function moneyText(value?: number) {
  return value == null ? '-' : `¥${Number(value).toFixed(2)}`;
}

function buildView(part: Part): PartDetailView {
  const isOfficial = part.source === 'OFFICIAL';
  return {
    ...part,
    sourceText: isOfficial ? '官方配件' : '第三方配件',
    officialPartNoLabel: isOfficial ? '官方品号' : '参考官方品号',
    costPriceText: moneyText(part.costPrice),
    salePriceText: moneyText(part.salePrice),
    statusText: (part as any).status === 'DISABLED' ? '已停用' : '启用中'
  };
}

Page({
  data: {
    partId: '',
    part: null as PartDetailView | null,
    loading: false
  },

  onLoad(options: any) {
    if (!requireLogin('/pages/parts/index')) return;
    if (!requireAnyPermission(PART_PAGE_PERMISSIONS, '当前账号无权查看配件')) return;

    if (!options.id) {
      wx.showToast({ title: '缺少配件ID', icon: 'none' });
      return;
    }
    this.setData({ partId: options.id });
    this.fetchData();
  },

  async fetchData() {
    if (!this.data.partId) return;
    this.setData({ loading: true });
    try {
      const res = await getPartDetail(this.data.partId);
      this.setData({ part: buildView(res.data), loading: false });
    } catch (e) {
      this.setData({ loading: false });
      showRequestErrorToast(e, '配件加载失败，请返回列表刷新后重试');
    }
  }
});
