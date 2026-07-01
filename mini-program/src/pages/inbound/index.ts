import { getParts, lookupPartByCode } from '../../api/parts';
import { dictItemLabels, getDictItems } from '../../api/dict';
import { inboundInventory, createPartAndInbound } from '../../api/inventory';
import { Part, PartLookupResult } from '../../types/parts';
import { InboundRequest, InboundResponse, CreatePartAndInboundResponse } from '../../types/inventory';
import Toast from 'tdesign-miniprogram/toast/index';
import { normalizeSearchParam } from '../../utils/searchParams';
import { hasPermission, requireAnyPermission, requireLogin } from '../../utils/permission';
import { showRequestErrorToast } from '../../utils/requestError';

let partSearchTimer: number | undefined;

Page({
  data: {
    selectedPart: null as Part | null,
    lookupResult: null as PartLookupResult | null,
    scanUnmatchedCode: '',
    formData: {
      quantity: '',
      unitCost: '',
      barcode: '',
      locationRemark: '',
      reason: '',
      remark: ''
    },
    submitting: false,
    successResult: null as (InboundResponse | CreatePartAndInboundResponse) | null,
    
    // Popup state
    partSelectorVisible: false,
    partList: [] as Part[],
    allParts: [] as Part[],

    // Create part and inbound popup state
    createPartVisible: false,
    createPartFormData: {
      source: 'THIRD_PARTY',
      partName: '',
      officialPartNo: '',
      externalBarcode: '',
      model: '',
      categoryCode: '',
      costPrice: '',
      salePrice: '',
      inboundQuantity: '1',
      unitCost: '',
      locationRemark: '',
      reason: '扫码新增配件入库',
      remark: ''
    },
    createPartSubmitting: false,
    hasCreatePartPermission: false,
    partCategoryOptions: [] as string[],
    inboundReasonOptions: [] as string[]
  },

  onLoad() {
    if (!requireLogin('/pages/inbound/index')) return;
    if (!requireAnyPermission(['INVENTORY_INBOUND'], '当前账号无权进行配件入库')) return;
    this.refreshPermissions();
    this.loadParts();
    this.loadDictionaryOptions();
  },

  onShow() {
    if (!requireLogin('/pages/inbound/index')) return;
    if (!requireAnyPermission(['INVENTORY_INBOUND'], '当前账号无权进行配件入库')) return;
    this.refreshPermissions();
  },

  refreshPermissions() {
    this.setData({
      hasCreatePartPermission: hasPermission('PART_CREATE') || hasPermission('PART_MANAGE')
    });
  },

  loadDictionaryOptions() {
    Promise.all([
      getDictItems('PART_CATEGORY').catch(() => ({ data: [] })),
      getDictItems('INBOUND_REASON').catch(() => ({ data: [] }))
    ]).then(([partCategories, inboundReasons]) => {
      this.setData({
        partCategoryOptions: dictItemLabels(partCategories.data, []),
        inboundReasonOptions: dictItemLabels(inboundReasons.data, ['采购入库', '初始入库', '扫码新增配件入库', '其他'])
      });
    });
  },

  loadParts(keyword?: string) {
    const normalizedKeyword = normalizeSearchParam(keyword);
    getParts(normalizedKeyword ? { keyword: normalizedKeyword } : undefined).then(res => {
      // 真实后端已过滤 DISABLED
      const parts = res.data.records || [];
      this.setData({
        allParts: parts,
        partList: parts
      });
    }).catch(err => {
      console.error(err);
      showRequestErrorToast(err, '配件列表加载失败，请稍后重试');
    });
  },

  showPartSelector() {
    this.setData({ partSelectorVisible: true });
  },

  onPopupVisibleChange(e: any) {
    this.setData({ partSelectorVisible: e.detail.visible });
  },

  onSearchPart(e: any) {
    const keyword = normalizeSearchParam(e.detail.value);
    if (partSearchTimer) {
      clearTimeout(partSearchTimer);
    }
    partSearchTimer = setTimeout(() => {
      this.loadParts(keyword);
    }, 300) as unknown as number;
  },

  selectPart(e: any) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      selectedPart: item,
      lookupResult: null,
      scanUnmatchedCode: '',
      partSelectorVisible: false,
      'formData.unitCost': item.costPrice != null ? String(item.costPrice) : ''
    });
  },

  handleScan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (scanRes) => {
        const scanValue = (scanRes.result || '').trim();
        if (!scanValue) {
          Toast({ context: this, selector: '#t-toast', message: '未读取到条码', icon: 'close-circle' });
          return;
        }
        this.lookupScannedPart(scanValue);
      },
      fail: () => {
        Toast({ context: this, selector: '#t-toast', message: '扫码已取消', icon: 'close-circle' });
      }
    });
  },

  setScannedCreatePartContext(scanValue: string) {
    this.setData({
      'formData.barcode': scanValue,
      'createPartFormData.externalBarcode': scanValue,
      'createPartFormData.unitCost': '',
      'createPartFormData.officialPartNo': this.data.createPartFormData.source === 'OFFICIAL'
        ? (this.data.createPartFormData.officialPartNo || scanValue)
        : this.data.createPartFormData.officialPartNo
    });
  },

  lookupScannedPart(scanValue: string) {
    lookupPartByCode(scanValue).then(res => {
      const result = res.data;
      if (!result.matched || !result.partId) {
        this.setData({
          selectedPart: null,
          lookupResult: null,
          scanUnmatchedCode: scanValue
        });
        this.setScannedCreatePartContext(scanValue);
        Toast({ context: this, selector: '#t-toast', message: '未找到匹配配件', icon: 'close-circle' });
        return;
      }
      const part: Part = {
        id: result.partId,
        partCode: result.partCode || '',
        partName: result.partName || result.name || '',
        source: result.source || '',
        officialPartNo: result.officialPartNo,
        defaultBarcode: result.defaultBarcode,
        model: result.model,
        categoryCode: result.categoryCode || result.category || '',
        costPrice: result.costPrice,
        salePrice: result.salePrice
      };
      this.setData({
        selectedPart: part,
        lookupResult: {
          ...result,
          matchTypeText: this.getMatchTypeText(result.matchType)
        } as any,
        scanUnmatchedCode: '',
        'formData.barcode': scanValue,
        'formData.unitCost': part.costPrice != null ? String(part.costPrice) : ''
      });
      Toast({ context: this, selector: '#t-toast', message: '已识别配件', icon: 'check-circle' });
    }).catch((error: Error) => {
      this.setData({
        selectedPart: null,
        lookupResult: null,
        scanUnmatchedCode: '',
        'formData.barcode': scanValue
      });
      showRequestErrorToast(error, '扫码识别失败，请稍后重试或手动选择配件');
    });
  },

  getMatchTypeText(matchType?: string) {
    const map: Record<string, string> = {
      SYSTEM_BARCODE: '系统条码命中',
      EXTERNAL_BARCODE: '外部条码命中',
      PART_CODE: '配件编码命中',
      OFFICIAL_PART_NO: '官方品号命中',
      DEFAULT_BARCODE: '默认条码命中'
    };
    return matchType ? (map[matchType] || '扫码命中') : '扫码命中';
  },

  showCreatePartForm() {
    if (!this.data.hasCreatePartPermission) {
      Toast({ context: this, selector: '#t-toast', message: '新增配件需门店管理员授权', icon: 'close-circle' });
      return;
    }
    const barcode = (this.data.scanUnmatchedCode || this.data.formData.barcode || '').trim();
    if (barcode) {
      this.setScannedCreatePartContext(barcode);
    }
    this.setData({ createPartVisible: true });
  },

  onCreatePartPopupChange(e: any) {
    this.setData({ createPartVisible: e.detail.visible });
  },

  onCreatePartSourceChange(e: any) {
    this.updateCreatePartSource(e.detail.value);
  },

  onTapCreatePartSource(e: any) {
    this.updateCreatePartSource(e.currentTarget.dataset.source);
  },

  updateCreatePartSource(source: string) {
    if (source === 'OFFICIAL') {
      this.setData({
        'createPartFormData.source': source,
        'createPartFormData.officialPartNo': this.data.createPartFormData.officialPartNo || this.data.createPartFormData.externalBarcode || ''
      });
      return;
    }

    this.setData({
      'createPartFormData.source': 'THIRD_PARTY'
    });
  },

  onCreatePartNameChange(e: any) { this.setData({ 'createPartFormData.partName': e.detail.value }); },
  onCreatePartOfficialPartNoChange(e: any) { this.setData({ 'createPartFormData.officialPartNo': e.detail.value }); },
  onCreatePartModelChange(e: any) { this.setData({ 'createPartFormData.model': e.detail.value }); },
  onCreatePartCategoryChange(e: any) { this.setData({ 'createPartFormData.categoryCode': e.detail.value }); },
  onSelectCreatePartCategory(e: any) { this.setData({ 'createPartFormData.categoryCode': e.currentTarget.dataset.value }); },
  onCreatePartCostPriceChange(e: any) { this.setData({ 'createPartFormData.costPrice': e.detail.value }); },
  onCreatePartSalePriceChange(e: any) { this.setData({ 'createPartFormData.salePrice': e.detail.value }); },
  onCreatePartInboundQtyChange(e: any) { this.setData({ 'createPartFormData.inboundQuantity': e.detail.value }); },
  onCreatePartUnitCostChange(e: any) { this.setData({ 'createPartFormData.unitCost': e.detail.value }); },
  onCreatePartLocationChange(e: any) { this.setData({ 'createPartFormData.locationRemark': e.detail.value }); },
  onCreatePartReasonChange(e: any) { this.setData({ 'createPartFormData.reason': e.detail.value }); },
  onSelectCreatePartReason(e: any) { this.setData({ 'createPartFormData.reason': e.currentTarget.dataset.value }); },
  onCreatePartRemarkChange(e: any) { this.setData({ 'createPartFormData.remark': e.detail.value }); },

  cancelCreatePart() {
    this.setData({ createPartVisible: false });
  },

  submitCreatePartAndInbound() {
    if (this.data.createPartSubmitting) return;

    const formData = this.data.createPartFormData;
    if (!formData.partName.trim()) {
      Toast({ context: this, selector: '#t-toast', message: '请输入配件名称', icon: 'close-circle' });
      return;
    }

    if (formData.source === 'OFFICIAL' && !formData.officialPartNo.trim()) {
      Toast({ context: this, selector: '#t-toast', message: '官方配件必须填写官方品号', icon: 'close-circle' });
      return;
    }

    const inboundQty = parseInt(formData.inboundQuantity, 10);
    if (isNaN(inboundQty) || inboundQty <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '入库数量必须大于0', icon: 'close-circle' });
      return;
    }

    const unitCost = formData.unitCost ? parseFloat(formData.unitCost) : undefined;
    if (unitCost !== undefined && (isNaN(unitCost) || unitCost < 0)) {
      Toast({ context: this, selector: '#t-toast', message: '入库单价不能为负数', icon: 'close-circle' });
      return;
    }

    const costPrice = formData.costPrice ? parseFloat(formData.costPrice) : undefined;
    const salePrice = formData.salePrice ? parseFloat(formData.salePrice) : undefined;

    this.setData({ createPartSubmitting: true });

    createPartAndInbound({
      source: formData.source,
      partName: formData.partName.trim(),
      officialPartNo: formData.officialPartNo.trim() || undefined,
      externalBarcode: formData.externalBarcode || undefined,
      model: formData.model.trim() || undefined,
      categoryCode: formData.categoryCode.trim() || undefined,
      costPrice,
      salePrice,
      inboundQuantity: inboundQty,
      unitCost,
      locationRemark: formData.locationRemark.trim() || undefined,
      reason: formData.reason.trim() || undefined,
      remark: formData.remark.trim() || undefined
    }).then(res => {
      const result = res.data;
      this.setData({
        createPartSubmitting: false,
        createPartVisible: false,
        selectedPart: {
          id: result.partId,
          partCode: result.partCode || '',
          partName: result.partName || '',
          source: formData.source,
          officialPartNo: formData.officialPartNo.trim() || undefined,
          defaultBarcode: result.defaultBarcode,
          model: formData.model || '',
          categoryCode: formData.categoryCode || '',
          costPrice: costPrice,
          salePrice: salePrice
        },
        successResult: {
          partId: result.partId,
          partCode: result.partCode || '',
          partName: result.partName || '',
          defaultBarcode: result.defaultBarcode,
          externalBarcode: result.externalBarcode,
          actualQty: result.actualQty,
          availableQty: result.availableQty,
          reservedQty: result.reservedQty,
          flowId: result.flowId,
          operatedAt: result.operatedAt
        }
      });
      Toast({ context: this, selector: '#t-toast', message: '新增配件并入库成功', icon: 'check-circle' });
    }).catch((err: any) => {
      this.setData({ createPartSubmitting: false });
      const message = err?.message || '操作失败';
      Toast({ context: this, selector: '#t-toast', message, icon: 'close-circle' });
    });
  },

  onQuantityChange(e: any) { this.setData({ 'formData.quantity': e.detail.value }); },
  onUnitCostChange(e: any) { this.setData({ 'formData.unitCost': e.detail.value }); },
  onBarcodeChange(e: any) {
    const barcode = e.detail.value;
    this.setData({
      scanUnmatchedCode: '',
      'formData.barcode': barcode,
      'createPartFormData.externalBarcode': barcode
    });
  },
  onLocationChange(e: any) { this.setData({ 'formData.locationRemark': e.detail.value }); },
  onReasonChange(e: any) { this.setData({ 'formData.reason': e.detail.value }); },
  onSelectInboundReason(e: any) { this.setData({ 'formData.reason': e.currentTarget.dataset.value }); },
  onRemarkChange(e: any) { this.setData({ 'formData.remark': e.detail.value }); },

  submitInbound() {
    if (this.data.submitting) return;

    if (!this.data.selectedPart) {
      Toast({ context: this, selector: '#t-toast', message: '请选择配件', icon: 'close-circle' });
      return;
    }

    const qty = parseInt(this.data.formData.quantity, 10);
    if (isNaN(qty) || qty <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入正确入库数量', icon: 'close-circle' });
      return;
    }

    const req: InboundRequest = {
      partId: this.data.selectedPart.id,
      quantity: qty
    };

    if (this.data.formData.unitCost) {
      const cost = parseFloat(this.data.formData.unitCost);
      if (isNaN(cost) || cost < 0) {
        Toast({ context: this, selector: '#t-toast', message: '入库单价不能为负数', icon: 'close-circle' });
        return;
      }
      req.unitCost = cost;
    }

    if (this.data.formData.barcode) req.barcode = this.data.formData.barcode;
    if (this.data.formData.locationRemark) req.locationRemark = this.data.formData.locationRemark;
    if (this.data.formData.reason) req.reason = this.data.formData.reason;
    if (this.data.formData.remark) req.remark = this.data.formData.remark;

    this.setData({ submitting: true });

    inboundInventory(req).then(res => {
      this.setData({
        submitting: false,
        successResult: res.data,
        lookupResult: this.data.lookupResult ? {
          ...this.data.lookupResult,
          actualQty: res.data.actualQty,
          availableQty: res.data.availableQty,
          reservedQty: res.data.reservedQty
        } : null
      });
      Toast({ context: this, selector: '#t-toast', message: '入库成功', icon: 'check-circle' });
    }).catch(err => {
      this.setData({ submitting: false });
      showRequestErrorToast(err, '入库失败，请检查配件、数量和单价后重试');
    });
  },

  resetForm() {
    this.setData({
      selectedPart: null,
      lookupResult: null,
      scanUnmatchedCode: '',
      formData: {
        quantity: '',
        unitCost: '',
        barcode: '',
        locationRemark: '',
        reason: '',
        remark: ''
      },
      successResult: null,
      createPartFormData: {
        source: 'THIRD_PARTY',
        partName: '',
        officialPartNo: '',
        externalBarcode: '',
        model: '',
        categoryCode: '',
        costPrice: '',
        salePrice: '',
        inboundQuantity: '1',
        unitCost: '',
        locationRemark: '',
        reason: '扫码新增配件入库',
        remark: ''
      }
    });
  }
});
