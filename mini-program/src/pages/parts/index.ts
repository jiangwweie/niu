import { getParts } from '../../api/parts';
import { Part } from '../../types/parts';
import { normalizeSearchParam } from '../../utils/searchParams';
import { requireAnyPermission, requireLogin } from '../../utils/permission';
import { showRequestErrorToast } from '../../utils/requestError';

let partSearchTimer: number | undefined;
const PART_PAGE_PERMISSIONS = ['PART_VIEW', 'PART_MANAGE', 'PART_CREATE'];

Page({
  data: {
    keyword: '',
    parts: [] as Part[],
    loading: false,
  },
  onLoad() {
    if (!requireLogin('/pages/parts/index')) return;
    if (!requireAnyPermission(PART_PAGE_PERMISSIONS, '当前账号无权查看配件')) return;
    this.fetchData();
  },
  onShow() {
    if (!requireLogin('/pages/parts/index')) return;
    requireAnyPermission(PART_PAGE_PERMISSIONS, '当前账号无权查看配件');
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
      const keyword = normalizeSearchParam(this.data.keyword);
      const res = await getParts(keyword ? { keyword } : undefined);
      this.setData({ parts: res.data.records, loading: false });
    } catch (e) {
      this.setData({ loading: false });
      showRequestErrorToast(e, '配件加载失败，请稍后重试');
    }
  },
  onTapDetail(e: any) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/part-detail/index?id=${id}` });
  }
});
