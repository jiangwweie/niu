import { getParts, lookupPartByCode } from '../../api/parts';
import { getInventoryStockDetail } from '../../api/inventory';
import { searchCustomers, searchVehicles, CustomerSearchResult, VehicleSearchResult } from '../../api/customer';
import { authStore } from '../../stores/auth';
import {
  createDraftWorkOrder,
  updateDraftWorkOrder,
  getWorkOrderDetail,
  addChargeItem,
  addTempPartCharge,
  updateChargeItem,
  deleteChargeItem,
  submitWorkOrder,
  cancelWorkOrder,
  recordPayment,
  recordRefund,
  markRepairDone,
  deliverWorkOrder
} from '../../api/workOrder';
import { Part, PartLookupResult } from '../../types/parts';
import {
  WorkOrder,
  WorkOrderItem,
  CreateDraftWorkOrderRequest,
  UpdateDraftWorkOrderRequest,
  AddChargeItemRequest,
  UpdateChargeItemRequest,
  PaymentMethod
} from '../../types/workOrder';

import Toast from 'tdesign-miniprogram/toast/index';
import Dialog from 'tdesign-miniprogram/dialog/index';
import {
  getCashierStatusText,
  getInventoryStatusText,
  getNoChargeReasonText,
  getProgressStatusText,
} from '../../utils/statusText';

const NO_CHARGE_REASONS = ['官方售后', '免费检测', '老板免单', '质保处理', '其他'];
let customerSearchTimer: number | undefined;

function partFromLookup(result: PartLookupResult): Part {
  return {
    id: result.partId || '',
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
}

function resolveStatusText(d: WorkOrder | null): string {
  if (!d) return '';
  return getProgressStatusText(d.progressStatus || d.status, d.progressStatusText);
}

function getDraftCashierText(order: WorkOrder) {
  const hasItems = !!order.chargeItems?.length;
  return hasItems || Number(order.receivableAmount || 0) > 0 ? '草稿未提交' : '待录入费用';
}

Page({
  data: {
    step: 1,
    statusText: '新建中',
    workOrderId: null as string | number | null,
    workOrder: null as WorkOrder | null,
    editMode: false,
    canEditDraft: false,

    draft: {
      customerNameSnapshot: '',
      customerPhoneSnapshot: '',
      vehicleModelSnapshot: '',
      frameNoSnapshot: '',
      batteryNoSnapshot: '',
      repairItem: '',
      remark: ''
    },
    saving: false,

    // Customer/Vehicle search
    selectedCustomerId: null as number | null,
    selectedVehicleId: null as number | null,
    customerSearchResults: [] as CustomerSearchResult[],
    vehicleSearchResults: [] as VehicleSearchResult[],
    customerSearchVisible: false,
    vehicleSearchVisible: false,
    customerSearchKeyword: '',
    vehicleSearchKeyword: '',
    customerSearchState: 'idle',
    customerSearchError: '',

    // Charge Item Form
    itemPopupVisible: false,
    editingItemId: null as string | number | null,
    savingItem: false,
    currentItemForm: {
      chargeType: '',
      itemName: '',
      quantity: '',
      unit: '',
      unitPrice: '',
      remark: ''
    },

    // Part Selector
    partSelectorVisible: false,
    selectedPart: null as Part | null,
    partList: [] as Part[],
    allParts: [] as Part[],
    selectedPartStock: null as { hasStockRecord: boolean; actualQty: number; availableQty: number; reservedQty: number } | null,
    deletingItem: false,
    lastScannedCode: '',

    // Temporary part
    tempPartDialogVisible: false,
    tempPartSaving: false,
    tempPartForm: {
      partName: '',
      barcode: '',
      model: '',
      categoryCode: '',
      quantity: '1',
      unit: '件',
      unitPrice: '',
      unitCost: '',
      locationRemark: '',
      remark: ''
    },

    // Submit
    submitDialogVisible: false,
    submitRemark: '',
    submitLoading: false,

    // Cancel
    cancelDialogVisible: false,
    cancelReason: '',
    cancelRemark: '',
    cancelLoading: false,

    // Payment
    paymentDialogVisible: false,
    paymentForm: {
      amount: '',
      paymentMethod: 'WECHAT' as PaymentMethod,
      remark: ''
    },
    paymentLoading: false,

    // Refund
    refundDialogVisible: false,
    refundForm: {
      amount: '',
      refundMethod: 'WECHAT' as PaymentMethod,
      reason: '',
      remark: ''
    },
    refundLoading: false,

    // Mark Repair Done
    markRepairDoneDialogVisible: false,
    markRepairDoneLoading: false,
    noChargeReason: '',
    noChargeRemark: '',
    noChargeReasons: NO_CHARGE_REASONS,

    // Deliver
    deliverDialogVisible: false,
    deliverConfirmText: '',
    deliverRemark: '',
    deliverLoading: false,
    canShowRecordPayment: false,
    canShowRecordRefund: false,
    showPaidInFullHint: false,
    outstandingAmount: 0,
    outstandingAmountStr: '0.00',
    refundableAmount: 0,
    refundableAmountStr: '0.00',
  },

  onLoad(options?: { id?: string; workOrderId?: string }) {
    this.loadParts();
    const workOrderId = options?.id || options?.workOrderId;
    if (workOrderId) {
      this.setData({
        workOrderId,
        editMode: true,
        step: 2
      });
      this.refreshWorkOrder();
    }
  },

  onShow() {
    if (!authStore.isLoggedIn) {
      wx.redirectTo({ url: '/pages/login/index?redirect=' + encodeURIComponent('/pages/create-work-order-placeholder/index') });
      return;
    }
    if (this.data.workOrderId) {
      this.refreshWorkOrder();
    }
  },

  loadParts() {
    getParts().then(res => {
      this.setData({
        allParts: res.data.records,
        partList: res.data.records
      });
    }).catch(console.error);
  },

  onDraftChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`draft.${field}`]: e.detail.value });
  },

  saveDraft() {
    if (this.data.saving) return;

    if (!this.data.draft.customerNameSnapshot) {
      Toast({ context: this, selector: '#t-toast', message: '客户姓名必填', icon: 'close-circle' });
      return;
    }
    if (!this.data.draft.repairItem) {
      Toast({ context: this, selector: '#t-toast', message: '维修项目必填', icon: 'close-circle' });
      return;
    }

    this.setData({ saving: true });

    const req: CreateDraftWorkOrderRequest | UpdateDraftWorkOrderRequest = {
      ...this.data.draft,
      customerId: this.data.selectedCustomerId || undefined,
      vehicleId: this.data.selectedVehicleId || undefined
    };

    if (this.data.workOrderId) {
      updateDraftWorkOrder(this.data.workOrderId, req as UpdateDraftWorkOrderRequest).then(() => {
        this.setData({
          saving: false,
          step: 2
        });
        Toast({ context: this, selector: '#t-toast', message: '草稿已更新', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(() => {
        this.setData({ saving: false });
      });
      return;
    }

    createDraftWorkOrder(req as CreateDraftWorkOrderRequest).then(res => {
      this.setData({
        saving: false,
        step: 2,
        workOrderId: res.data.id
      });
      Toast({ context: this, selector: '#t-toast', message: '工单草稿已创建', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      this.setData({ saving: false });
    });
  },

  refreshWorkOrder() {
    if (!this.data.workOrderId) return;
    getWorkOrderDetail(this.data.workOrderId).then(res => {
      const d = res.data;
      const workOrder = {
        ...d,
        progressStatusText: getProgressStatusText(d?.progressStatus || d?.status, d?.progressStatusText),
        cashierStatusText: (d?.progressStatus || d?.status) === 'DRAFT' && d?.cashierStatus === 'NO_CHARGE'
          ? getDraftCashierText(d)
          : getCashierStatusText(d?.cashierStatus, d?.cashierStatusText),
        inventoryStatusText: getInventoryStatusText(d?.inventoryStatus, d?.inventoryStatusText),
        noChargeReasonText: getNoChargeReasonText(d?.noChargeReason),
      };
      const status = d?.progressStatus || d?.status;
      this.setData({
        workOrder,
        statusText: resolveStatusText(workOrder),
        canEditDraft: status === 'DRAFT',
        ...this.resolveDraftState(d),
        ...this.resolveActionState(workOrder)
      });
      if (this.data.editMode && status !== 'DRAFT') {
        Toast({ context: this, selector: '#t-toast', message: '仅新建中工单可编辑', icon: 'close-circle' });
      }
    }).catch(console.error);
  },

  resolveDraftState(order: WorkOrder | null) {
    if (!order) return {};
    return {
      selectedCustomerId: typeof order.customerId === 'number' ? order.customerId : null,
      selectedVehicleId: typeof order.vehicleId === 'number' ? order.vehicleId : null,
      draft: {
        customerNameSnapshot: order.customerNameSnapshot || '',
        customerPhoneSnapshot: order.customerPhoneSnapshot || '',
        vehicleModelSnapshot: order.vehicleModelSnapshot || '',
        frameNoSnapshot: order.frameNoSnapshot || '',
        batteryNoSnapshot: order.batteryNoSnapshot || '',
        repairItem: order.repairItem || '',
        remark: order.remark || ''
      }
    };
  },

  editDraftInfo() {
    if (!this.data.canEditDraft) {
      Toast({ context: this, selector: '#t-toast', message: '仅新建中工单可编辑', icon: 'close-circle' });
      return;
    }
    this.setData({ step: 1 });
  },

  backToChargeItems() {
    if (!this.data.workOrderId) return;
    this.setData({ step: 2 });
    this.refreshWorkOrder();
  },

  resolveActionState(order: WorkOrder | null) {
    const status = order?.progressStatus || order?.status;
    const netReceived = Number(order?.netReceived ?? order?.receivedAmount ?? 0);
    const receivable = Number(order?.receivableAmount ?? 0);
    const outstanding = Math.max(Number(order?.outstandingAmount ?? (receivable - netReceived)), 0);
    const refundable = Math.max(Number(order?.refundableAmount ?? 0), 0);
    const paymentAllowedStatus = status === 'REPAIRING' || status === 'REPAIR_DONE';
    const canShowRecordPayment = !!order?.canRecordPayment && paymentAllowedStatus && outstanding > 0;
    return {
      canShowRecordPayment,
      canShowRecordRefund: !!order?.canRecordRefund && status !== 'DRAFT',
      showPaidInFullHint: paymentAllowedStatus && outstanding <= 0,
      outstandingAmount: outstanding,
      outstandingAmountStr: outstanding.toFixed(2),
      refundableAmount: refundable,
      refundableAmountStr: refundable.toFixed(2),
    };
  },

  openAddItem(e: any) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      itemPopupVisible: true,
      editingItemId: null,
      selectedPart: null,
      selectedPartStock: null,
      lastScannedCode: '',
      currentItemForm: {
        chargeType: type,
        itemName: type === 'LABOR' ? '维修工时费' : '',
        quantity: '1',
        unit: type === 'PART' ? '件' : (type === 'LABOR' ? '次' : ''),
        unitPrice: '',
        remark: ''
      }
    });
    if (type === 'PART') {
      Toast({ context: this, selector: '#t-toast', message: '配件需从配件库选择；手工输入请使用工时或其他费用。', icon: 'info-circle' });
    }
  },

  editItem(e: any) {
    const item: WorkOrderItem = e.currentTarget.dataset.item;
    this.setData({
      itemPopupVisible: true,
      editingItemId: item.id,
      selectedPart: null,
      selectedPartStock: null,
      currentItemForm: {
        chargeType: item.chargeType,
        itemName: item.itemName,
        quantity: String(item.quantity),
        unit: item.unit || '',
        unitPrice: String(item.unitPrice),
        remark: item.remark || ''
      }
    });
  },

  closeItemPopup() {
    this.setData({ itemPopupVisible: false });
  },

  onPopupVisibleChange(e: any) {
    this.setData({
      itemPopupVisible: e.detail.visible || false
    });
  },

  onItemFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    if (this.data.currentItemForm.chargeType === 'PART' && field === 'itemName') {
      Toast({ context: this, selector: '#t-toast', message: '配件需从配件库选择；手工输入请使用工时或其他费用。', icon: 'close-circle' });
      return;
    }
    this.setData({ [`currentItemForm.${field}`]: e.detail.value });
  },

  saveChargeItem() {
    if (this.data.savingItem || !this.data.workOrderId) return;

    const { chargeType, itemName, quantity, unit, unitPrice, remark } = this.data.currentItemForm;

    if (!itemName) {
      Toast({ context: this, selector: '#t-toast', message: '项目名称必填', icon: 'close-circle' });
      return;
    }

    if (chargeType === 'PART' && !this.data.editingItemId && !this.data.selectedPart) {
      Toast({ context: this, selector: '#t-toast', message: '配件必须从配件库选择', icon: 'close-circle' });
      return;
    }

    const qty = Number(quantity);
    if (!Number.isInteger(qty) || qty <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入正确的整数数量', icon: 'close-circle' });
      return;
    }

    const price = parseFloat(unitPrice);
    if (isNaN(price) || price < 0) {
      Toast({ context: this, selector: '#t-toast', message: '单价不能小于0', icon: 'close-circle' });
      return;
    }

    if (chargeType === 'PART' && !this.data.editingItemId) {
      const stock = this.data.selectedPartStock;
      if (!stock?.hasStockRecord) {
        Toast({ context: this, selector: '#t-toast', message: '该配件暂未入库，请先完成入库操作，或使用临时新增配件并入库。', icon: 'close-circle' });
        return;
      }
      if (stock.availableQty <= 0) {
        Toast({ context: this, selector: '#t-toast', message: '该配件暂无可用库存，请先入库或调整配件。', icon: 'close-circle' });
        return;
      }
      if (qty > stock.availableQty) {
        Toast({ context: this, selector: '#t-toast', message: '可用库存不足，当前可用 ' + stock.availableQty + '。', icon: 'close-circle' });
        return;
      }
    }

    this.setData({ savingItem: true });

    if (this.data.editingItemId) {
      const req: UpdateChargeItemRequest = {
        itemName,
        quantity: qty,
        unit,
        unitPrice: price,
        remark
      };
      updateChargeItem(this.data.workOrderId, this.data.editingItemId, req).then(() => {
        this.setData({ savingItem: false, itemPopupVisible: false });
        Toast({ context: this, selector: '#t-toast', message: '费用明细已更新', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(() => {
        this.setData({ savingItem: false });
      });
    } else {
      const req: AddChargeItemRequest = {
        chargeType,
        itemName,
        quantity: qty,
        unit,
        unitPrice: price,
        remark
      };
      if (chargeType === 'PART' && this.data.selectedPart) {
        req.partId = this.data.selectedPart.id;
      }

      addChargeItem(this.data.workOrderId, req).then(() => {
        this.setData({ savingItem: false, itemPopupVisible: false });
        Toast({ context: this, selector: '#t-toast', message: '费用明细已添加。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(() => {
        this.setData({ savingItem: false });
      });
    }
  },

  confirmDeleteItem(e: any) {
    if (this.data.deletingItem) return;
    const id = e.currentTarget.dataset.id;
    Dialog.confirm({
      title: '删除确认',
      content: '确定要删除该费用明细吗？',
      confirmBtn: '删除',
      cancelBtn: '取消',
    }).then(() => {
      if (!this.data.workOrderId) return;
      this.setData({ deletingItem: true });
      deleteChargeItem(this.data.workOrderId, id).then(() => {
        Toast({ context: this, selector: '#t-toast', message: '费用明细已删除。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(console.error)
      .finally(() => {
        this.setData({ deletingItem: false });
      });
    }).catch(() => {});
  },

  // Part Selector
  openPartSelector() {
    this.setData({ partSelectorVisible: true });
  },

  closePartSelector() {
    this.setData({ partSelectorVisible: false });
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
      'currentItemForm.itemName': item.partName,
      'currentItemForm.unitPrice': item.salePrice != null ? String(item.salePrice) : '',
      lastScannedCode: '',
      partSelectorVisible: false
    });
    this.loadSelectedPartStock(item.id);
  },

  loadSelectedPartStock(partId: string | number) {
    getInventoryStockDetail(String(partId)).then(res => {
      this.setData({
        selectedPartStock: {
          hasStockRecord: true,
          actualQty: Number(res.data.actualQty || 0),
          availableQty: Number(res.data.availableQty || 0),
          reservedQty: Number(res.data.reservedQty || 0),
        }
      });
    }).catch(() => {
      this.setData({
        selectedPartStock: {
          hasStockRecord: false,
          actualQty: 0,
          availableQty: 0,
          reservedQty: 0,
        }
      });
    });
  },

  scanPartForChargeItem() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (scanRes) => {
        const scanValue = (scanRes.result || '').trim();
        if (!scanValue) {
          Toast({ context: this, selector: '#t-toast', message: '未读取到条码', icon: 'close-circle' });
          return;
        }
        lookupPartByCode(scanValue).then(res => {
          const result = res.data;
          if (!result.matched || !result.partId) {
            this.setData({
              lastScannedCode: scanValue,
              'tempPartForm.barcode': scanValue
            });
            wx.showModal({
              title: '未识别该条码',
              content: '可手动搜索已有配件，或临时新增配件并加入当前工单。',
              confirmText: '临时新增',
              cancelText: '手动搜索',
              success: modalRes => {
                if (modalRes.confirm) {
                  this.openTempPartDialog();
                } else {
                  this.openPartSelector();
                }
              }
            });
            return;
          }
          const part = partFromLookup(result);
          this.setData({
            selectedPart: part,
            selectedPartStock: {
              hasStockRecord: result.hasStockRecord !== false,
              actualQty: Number(result.actualQty || 0),
              availableQty: Number(result.availableQty || 0),
              reservedQty: Number(result.reservedQty || 0),
            },
            lastScannedCode: scanValue,
            'currentItemForm.itemName': part.partName,
            'currentItemForm.unitPrice': part.salePrice != null ? String(part.salePrice) : ''
          });
          Toast({ context: this, selector: '#t-toast', message: '已识别配件', icon: 'check-circle' });
        }).catch((error: Error) => {
          const message = error?.message || '';
          if (message.includes('已停用')) {
            Toast({ context: this, selector: '#t-toast', message, icon: 'close-circle' });
            return;
          }
          this.setData({
            lastScannedCode: scanValue,
            'tempPartForm.barcode': scanValue
          });
          wx.showModal({
            title: '未识别该条码',
            content: '可手动搜索已有配件，或临时新增配件并加入当前工单。',
            confirmText: '临时新增',
            cancelText: '手动搜索',
            success: modalRes => {
              if (modalRes.confirm) {
                this.openTempPartDialog();
              } else {
                this.openPartSelector();
              }
            }
          });
        });
      },
      fail: () => {
        Toast({ context: this, selector: '#t-toast', message: '扫码已取消', icon: 'close-circle' });
      }
    });
  },

  openTempPartDialog() {
    const quantity = this.data.currentItemForm.quantity || '1';
    const unit = this.data.currentItemForm.unit || '件';
    const unitPrice = this.data.currentItemForm.unitPrice || '';
    this.setData({
      tempPartDialogVisible: true,
      tempPartForm: {
        ...this.data.tempPartForm,
        barcode: this.data.lastScannedCode || this.data.tempPartForm.barcode,
        quantity,
        unit,
        unitPrice
      }
    });
  },

  closeTempPartDialog() {
    this.setData({ tempPartDialogVisible: false });
  },

  onTempPartPopupVisibleChange(e: any) {
    this.setData({ tempPartDialogVisible: e.detail.visible || false });
  },

  onTempPartFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`tempPartForm.${field}`]: e.detail.value });
  },

  submitTempPartCharge() {
    if (this.data.tempPartSaving || !this.data.workOrderId) return;
    const form = this.data.tempPartForm;
    if (!form.partName || form.partName.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入配件名称', icon: 'close-circle' });
      return;
    }
    const qty = Number(form.quantity);
    if (!Number.isInteger(qty) || qty <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入正确的整数数量', icon: 'close-circle' });
      return;
    }
    const unitPrice = parseFloat(form.unitPrice);
    if (isNaN(unitPrice) || unitPrice < 0) {
      Toast({ context: this, selector: '#t-toast', message: '销售单价不能小于0', icon: 'close-circle' });
      return;
    }
    let unitCost: number | undefined;
    if (form.unitCost) {
      unitCost = parseFloat(form.unitCost);
      if (isNaN(unitCost) || unitCost < 0) {
        Toast({ context: this, selector: '#t-toast', message: '单位成本不能小于0', icon: 'close-circle' });
        return;
      }
    }

    this.setData({ tempPartSaving: true });
    addTempPartCharge(this.data.workOrderId, {
      partName: form.partName,
      barcode: form.barcode || undefined,
      model: form.model || undefined,
      categoryCode: form.categoryCode || undefined,
      quantity: qty,
      unit: form.unit || '件',
      unitPrice,
      unitCost,
      locationRemark: form.locationRemark || undefined,
      remark: form.remark || undefined
    }).then(() => {
      this.setData({
        tempPartDialogVisible: false,
        itemPopupVisible: false,
        selectedPart: null,
        lastScannedCode: '',
        tempPartForm: {
          partName: '',
          barcode: '',
          model: '',
          categoryCode: '',
          quantity: '1',
          unit: '件',
          unitPrice: '',
          unitCost: '',
          locationRemark: '',
          remark: ''
        }
      });
      Toast({ context: this, selector: '#t-toast', message: '临时配件已加入工单', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(console.error)
    .finally(() => {
      this.setData({ tempPartSaving: false });
    });
  },

  // --- Customer Search ---
  onCustomerSearchInput(e: any) {
    const value = e.detail.value;
    this.setData({ customerSearchKeyword: value });
    if (customerSearchTimer) {
      clearTimeout(customerSearchTimer);
    }
    customerSearchTimer = setTimeout(() => {
      if (this.data.customerSearchKeyword.trim()) {
        this.onCustomerSearch();
      } else {
        this.setData({ customerSearchState: 'idle', customerSearchResults: [], customerSearchError: '' });
      }
    }, 300) as unknown as number;
  },

  onCustomerSearch() {
    const keyword = this.data.customerSearchKeyword.trim();
    if (!keyword) {
      Toast({ context: this, selector: '#t-toast', message: '请输入客户姓名或手机号', icon: 'close-circle' });
      return;
    }
    this.setData({ customerSearchState: 'loading', customerSearchError: '' });
    searchCustomers(keyword).then(res => {
      const results = res.data || [];
      this.setData({ customerSearchResults: results, customerSearchState: results.length > 0 ? 'success' : 'empty' });
    }).catch((err: Error) => {
      this.setData({ customerSearchState: 'error', customerSearchError: err.message || '搜索失败，请稍后重试' });
    });
  },

  openCustomerSearch() {
    this.setData({
      customerSearchVisible: true,
      customerSearchKeyword: '',
      customerSearchResults: [],
      customerSearchState: 'idle',
      customerSearchError: ''
    });
  },

  closeCustomerSearch() {
    this.setData({ customerSearchVisible: false });
  },

  onCustomerSearchPopupChange(e: any) {
    this.setData({ customerSearchVisible: e.detail.visible || false });
  },

  selectCustomer(e: any) {
    const customer: CustomerSearchResult = e.currentTarget.dataset.item;
    this.setData({
      selectedCustomerId: customer.id,
      'draft.customerNameSnapshot': customer.name,
      'draft.customerPhoneSnapshot': customer.phone || '',
      customerSearchVisible: false
    });
  },

  clearSelectedCustomer() {
    this.setData({
      selectedCustomerId: null,
      'draft.customerNameSnapshot': '',
      'draft.customerPhoneSnapshot': ''
    });
  },

  // --- Vehicle Search ---
  onVehicleSearchInput(e: any) {
    this.setData({ vehicleSearchKeyword: e.detail.value });
  },

  onVehicleSearch() {
    const keyword = this.data.vehicleSearchKeyword.trim();
    if (!keyword) {
      Toast({ context: this, selector: '#t-toast', message: '请输入车架号、车型或客户手机号', icon: 'close-circle' });
      return;
    }
    searchVehicles(keyword, this.data.selectedCustomerId).then(res => {
      this.setData({ vehicleSearchResults: res.data || [] });
    }).catch(console.error);
  },

  openVehicleSearch() {
    this.setData({
      vehicleSearchVisible: true,
      vehicleSearchKeyword: '',
      vehicleSearchResults: []
    });
  },

  closeVehicleSearch() {
    this.setData({ vehicleSearchVisible: false });
  },

  onVehicleSearchPopupChange(e: any) {
    this.setData({ vehicleSearchVisible: e.detail.visible || false });
  },

  selectVehicle(e: any) {
    const vehicle: VehicleSearchResult = e.currentTarget.dataset.item;
    this.setData({
      selectedCustomerId: vehicle.customerId || this.data.selectedCustomerId,
      selectedVehicleId: vehicle.id,
      'draft.customerNameSnapshot': vehicle.customerName || this.data.draft.customerNameSnapshot,
      'draft.customerPhoneSnapshot': vehicle.customerPhone || this.data.draft.customerPhoneSnapshot,
      'draft.vehicleModelSnapshot': vehicle.model || '',
      'draft.frameNoSnapshot': vehicle.frameNo || '',
      'draft.batteryNoSnapshot': vehicle.batteryNo || '',
      vehicleSearchVisible: false
    });
  },

  clearSelectedVehicle() {
    this.setData({
      selectedVehicleId: null,
      'draft.vehicleModelSnapshot': '',
      'draft.frameNoSnapshot': '',
      'draft.batteryNoSnapshot': ''
    });
  },

  // --- Submit Work Order ---
  confirmSubmitWorkOrder() {
    if (!this.data.workOrderId || !this.data.workOrder) return;

    if (!this.data.workOrder.chargeItems || this.data.workOrder.chargeItems.length === 0) {
      Dialog.confirm({
        title: '缺少费用明细',
        content: '当前工单没有录入任何费用明细，是否继续提交？',
        confirmBtn: '继续提交',
        cancelBtn: '取消'
      }).then(() => {
        this.setData({ submitDialogVisible: true, submitRemark: '' });
      }).catch(() => {});
    } else {
      this.setData({ submitDialogVisible: true, submitRemark: '' });
    }
  },

  onSubmitRemarkChange(e: any) {
    this.setData({ submitRemark: e.detail.value });
  },

  onCancelSubmit() {
    this.setData({ submitDialogVisible: false });
  },

  onConfirmSubmit() {
    if (this.data.submitLoading || !this.data.workOrderId) return;

    this.setData({ submitLoading: true, submitDialogVisible: false });

    submitWorkOrder(this.data.workOrderId!, { remark: this.data.submitRemark }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '工单已提交，进入维修中。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Submit work order failed:', err);
    }).finally(() => {
      this.setData({ submitLoading: false });
    });
  },

  // --- Cancel Work Order ---
  openCancelDialog() {
    this.setData({
      cancelDialogVisible: true,
      cancelReason: '',
      cancelRemark: ''
    });
  },

  onCancelReasonChange(e: any) {
    this.setData({ cancelReason: e.detail.value });
  },

  onCancelRemarkChange(e: any) {
    this.setData({ cancelRemark: e.detail.value });
  },

  onCancelCancelDialog() {
    this.setData({ cancelDialogVisible: false });
  },

  onConfirmCancel() {
    if (this.data.cancelLoading || !this.data.workOrderId) return;

    if (!this.data.cancelReason || this.data.cancelReason.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入取消原因', icon: 'close-circle' });
      return;
    }

    this.setData({ cancelLoading: true, cancelDialogVisible: false });

    cancelWorkOrder(this.data.workOrderId!, {
      reason: this.data.cancelReason,
      remark: this.data.cancelRemark
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '工单已取消。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Cancel work order failed:', err);
    }).finally(() => {
      this.setData({ cancelLoading: false });
    });
  },

  // --- Record Payment ---
  openPaymentDialog() {
    const order = this.data.workOrder;
    if (!this.data.canShowRecordPayment) {
      if (this.data.outstandingAmount <= 0) {
        Toast({ context: this, selector: '#t-toast', message: '已无待收金额，无需继续收款', icon: 'close-circle' });
        return;
      }
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许记录收款', icon: 'close-circle' });
      return;
    }

    this.setData({
      paymentDialogVisible: true,
      paymentForm: {
        amount: '',
        paymentMethod: 'WECHAT',
        remark: ''
      }
    });
  },

  closePaymentDialog() {
    this.setData({ paymentDialogVisible: false });
  },

  onPaymentPopupVisibleChange(e: any) {
    this.setData({ paymentDialogVisible: e.detail.visible || false });
  },

  onPaymentFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`paymentForm.${field}`]: e.detail.value });
  },

  onSelectPaymentMethod(e: any) {
    this.setData({ 'paymentForm.paymentMethod': e.currentTarget.dataset.val });
  },

  confirmRecordPayment() {
    if (this.data.paymentLoading || !this.data.workOrderId) return;

    const amount = parseFloat(this.data.paymentForm.amount);
    if (isNaN(amount) || amount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的收款金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.paymentForm.paymentMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择收款方式', icon: 'close-circle' });
      return;
    }
    const max = this.data.outstandingAmount || 0;
    if (max <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '已无待收金额，无需继续收款', icon: 'close-circle' });
      return;
    }
    if (amount > max) {
      Toast({ context: this, selector: '#t-toast', message: '收款金额超过待收金额 ¥' + max.toFixed(2), icon: 'close-circle' });
      return;
    }

    this.setData({ paymentLoading: true });

    recordPayment(this.data.workOrderId!, {
      amount: amount,
      paymentMethod: this.data.paymentForm.paymentMethod,
      remark: this.data.paymentForm.remark
    }).then(() => {
      this.setData({ paymentDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '收款记录已保存。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Record payment failed:', err);
    }).finally(() => {
      this.setData({ paymentLoading: false });
    });
  },

  // --- Record Refund ---
  openRefundDialog() {
    const order = this.data.workOrder;
    if (!order?.canRecordRefund) {
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许记录退款', icon: 'close-circle' });
      return;
    }

    this.setData({
      refundDialogVisible: true,
      refundForm: {
        amount: '',
        refundMethod: 'WECHAT',
        reason: '',
        remark: ''
      }
    });
  },

  closeRefundDialog() {
    this.setData({ refundDialogVisible: false });
  },

  onRefundPopupVisibleChange(e: any) {
    this.setData({ refundDialogVisible: e.detail.visible || false });
  },

  onRefundFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`refundForm.${field}`]: e.detail.value });
  },

  onSelectRefundMethod(e: any) {
    this.setData({ 'refundForm.refundMethod': e.currentTarget.dataset.val });
  },

  confirmRecordRefund() {
    if (this.data.refundLoading || !this.data.workOrderId) return;

    const amount = parseFloat(this.data.refundForm.amount);
    if (isNaN(amount) || amount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的退款金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.refundForm.refundMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择退款方式', icon: 'close-circle' });
      return;
    }

    if (!this.data.refundForm.reason || this.data.refundForm.reason.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入退款原因', icon: 'close-circle' });
      return;
    }
    const maxRefundable = this.data.refundableAmount || 0;
    if (maxRefundable <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '当前无可退金额', icon: 'close-circle' });
      return;
    }
    if (amount > maxRefundable) {
      Toast({ context: this, selector: '#t-toast', message: '退款金额超过可退金额 ¥' + maxRefundable.toFixed(2), icon: 'close-circle' });
      return;
    }

    this.setData({ refundLoading: true });

    recordRefund(this.data.workOrderId!, {
      amount: amount,
      refundMethod: this.data.refundForm.refundMethod,
      reason: this.data.refundForm.reason,
      remark: this.data.refundForm.remark
    }).then(() => {
      this.setData({ refundDialogVisible: false });
      Toast({ context: this, selector: '#t-toast', message: '退款记录已保存。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Record refund failed:', err);
    }).finally(() => {
      this.setData({ refundLoading: false });
    });
  },

  // --- Mark Repair Done ---
  openMarkRepairDoneDialog() {
    const order = this.data.workOrder;
    if (!order?.canMarkRepairDone) {
      Toast({ context: this, selector: '#t-toast', message: '当前状态不允许标记维修完成', icon: 'close-circle' });
      return;
    }

    this.setData({
      markRepairDoneDialogVisible: true,
      noChargeReason: '',
      noChargeRemark: ''
    });
  },

  onCancelMarkRepairDone() {
    this.setData({ markRepairDoneDialogVisible: false });
  },

  onNoChargeReasonChange(e: any) {
    this.setData({ noChargeReason: e.detail.value });
  },

  onNoChargeRemarkChange(e: any) {
    this.setData({ noChargeRemark: e.detail.value });
  },

  onSelectNoChargeReason(e: any) {
    this.setData({ noChargeReason: e.currentTarget.dataset.val });
  },

  onConfirmMarkRepairDone() {
    if (this.data.markRepairDoneLoading || !this.data.workOrderId) return;

    const order = this.data.workOrder;
    const isZeroReceivable = (order?.receivableAmount ?? 0) <= 0;

    if (isZeroReceivable && !this.data.noChargeReason) {
      Toast({ context: this, selector: '#t-toast', message: '应收为0时必须选择无需收款原因', icon: 'close-circle' });
      return;
    }

    this.setData({ markRepairDoneLoading: true, markRepairDoneDialogVisible: false });

    const payload: any = {};
    if (isZeroReceivable) {
      payload.noChargeReason = this.data.noChargeReason;
      if (this.data.noChargeRemark) payload.noChargeRemark = this.data.noChargeRemark;
    }

    markRepairDone(this.data.workOrderId!, Object.keys(payload).length > 0 ? payload : undefined).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '已标记维修完成，库存已扣减。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Mark repair done failed:', err);
    }).finally(() => {
      this.setData({ markRepairDoneLoading: false });
    });
  },

  // --- Deliver ---
  openDeliverDialog() {
    const order = this.data.workOrder;
    if (!order?.canDeliver) {
      Toast({ context: this, selector: '#t-toast', message: '请先收齐尾款后再交付关闭', icon: 'close-circle' });
      return;
    }

    this.setData({
      deliverDialogVisible: true,
      deliverConfirmText: this.getDeliverConfirmText(),
      deliverRemark: ''
    });
  },

  onCancelDeliver() {
    this.setData({ deliverDialogVisible: false });
  },

  onDeliverRemarkChange(e: any) {
    this.setData({ deliverRemark: e.detail.value });
  },

  getDeliverConfirmText(): string {
    const order = this.data.workOrder;
    if (!order) return '';
    const cs = order.cashierStatus;
    if (cs === 'PAID') return '工单已结清。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。';
    if (cs === 'NO_CHARGE') {
      const reason = order.noChargeReason ? '（原因：' + getNoChargeReasonText(order.noChargeReason) + '）' : '';
      return '无需收款工单' + reason + '。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。';
    }
    return '确认交付关闭后，工单将进入已交付状态。';
  },

  onConfirmDeliver() {
    if (this.data.deliverLoading || !this.data.workOrderId) return;

    this.setData({ deliverLoading: true, deliverDialogVisible: false });

    deliverWorkOrder(this.data.workOrderId!, this.data.deliverRemark ? { remark: this.data.deliverRemark } : undefined).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '工单已交付关闭。', icon: 'check-circle' });
      this.refreshWorkOrder();
    }).catch(err => {
      console.error('Deliver work order failed:', err);
    }).finally(() => {
      this.setData({ deliverLoading: false });
    });
  }
});
