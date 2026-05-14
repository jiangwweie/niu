import { getParts } from '../../api/parts';
import { inboundInventory } from '../../api/inventory';
import { Part } from '../../types/parts';
import { InboundRequest, InboundResponse } from '../../types/inventory';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
  data: {
    selectedPart: null as Part | null,
    formData: {
      quantity: '',
      unitCost: '',
      barcode: '',
      locationRemark: '',
      reason: '',
      remark: ''
    },
    submitting: false,
    successResult: null as InboundResponse | null,
    
    // Popup state
    partSelectorVisible: false,
    partList: [] as Part[],
    allParts: [] as Part[]
  },

  onLoad() {
    this.loadParts();
  },

  loadParts() {
    getParts().then(res => {
      // 真实后端已过滤 DISABLED
      const parts = res.data.records;
      this.setData({
        allParts: parts,
        partList: parts
      });
    }).catch(err => {
      console.error(err);
    });
  },

  showPartSelector() {
    this.setData({ partSelectorVisible: true });
  },

  onPopupVisibleChange(e: any) {
    this.setData({ partSelectorVisible: e.detail.visible });
  },

  onSearchPart(e: any) {
    const keyword = e.detail.value.toLowerCase();
    const filtered = this.data.allParts.filter(p => 
      p.partName.toLowerCase().includes(keyword) || 
      p.partCode.toLowerCase().includes(keyword)
    );
    this.setData({ partList: filtered });
  },

  selectPart(e: any) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      selectedPart: item,
      partSelectorVisible: false
    });
  },

  handleScan() {
    wx.showModal({
      title: '提示',
      content: '扫码能力后续接入，当前请手动选择配件或输入条码。',
      showCancel: false
    });
  },

  onQuantityChange(e: any) { this.setData({ 'formData.quantity': e.detail.value }); },
  onUnitCostChange(e: any) { this.setData({ 'formData.unitCost': e.detail.value }); },
  onBarcodeChange(e: any) { this.setData({ 'formData.barcode': e.detail.value }); },
  onLocationChange(e: any) { this.setData({ 'formData.locationRemark': e.detail.value }); },
  onReasonChange(e: any) { this.setData({ 'formData.reason': e.detail.value }); },
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
        successResult: res.data
      });
      Toast({ context: this, selector: '#t-toast', message: '入库成功', icon: 'check-circle' });
    }).catch(err => {
      this.setData({ submitting: false });
    });
  },

  resetForm() {
    this.setData({
      selectedPart: null,
      formData: {
        quantity: '',
        unitCost: '',
        barcode: '',
        locationRemark: '',
        reason: '',
        remark: ''
      },
      successResult: null
    });
  }
});
