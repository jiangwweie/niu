import { getParts } from '../../api/parts';
import { searchCustomers, searchVehicles, CustomerSearchResult, VehicleSearchResult } from '../../api/customer';
import {
  createDraftWorkOrder,
  getWorkOrderDetail,
  addChargeItem,
  updateChargeItem,
  deleteChargeItem,
  submitWorkOrder,
  cancelWorkOrder,
  recordPayment,
  recordRefund,
  markRepairDone,
  deliverWorkOrder
} from '../../api/workOrder';
import { Part } from '../../types/parts';
import {
  WorkOrder,
  WorkOrderItem,
  CreateDraftWorkOrderRequest,
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

function resolveStatusText(d: WorkOrder | null): string {
  if (!d) return '';
  return getProgressStatusText(d.progressStatus || d.status, d.progressStatusText);
}

Page({
  data: {
    step: 1,
    statusText: '新建中',
    workOrderId: null as string | number | null,
    workOrder: null as WorkOrder | null,

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
    deletingItem: false,

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
  },

  onLoad() {
    this.loadParts();
  },

  onShow() {
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

    const req: CreateDraftWorkOrderRequest = {
      ...this.data.draft,
      customerId: this.data.selectedCustomerId || undefined,
      vehicleId: this.data.selectedVehicleId || undefined
    };

    createDraftWorkOrder(req).then(res => {
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
        cashierStatusText: getCashierStatusText(d?.cashierStatus, d?.cashierStatusText),
        inventoryStatusText: getInventoryStatusText(d?.inventoryStatus, d?.inventoryStatusText),
        noChargeReasonText: getNoChargeReasonText(d?.noChargeReason),
      };
      this.setData({
        workOrder,
        statusText: resolveStatusText(workOrder)
      });
    }).catch(console.error);
  },

  openAddItem(e: any) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      itemPopupVisible: true,
      editingItemId: null,
      selectedPart: null,
      currentItemForm: {
        chargeType: type,
        itemName: type === 'LABOR' ? '维修工时费' : '',
        quantity: '1',
        unit: type === 'PART' ? '件' : (type === 'LABOR' ? '次' : ''),
        unitPrice: '',
        remark: ''
      }
    });
  },

  editItem(e: any) {
    const item: WorkOrderItem = e.currentTarget.dataset.item;
    this.setData({
      itemPopupVisible: true,
      editingItemId: item.id,
      selectedPart: null,
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
      Toast({ context: this, selector: '#t-toast', message: '配件必须选择', icon: 'close-circle' });
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
      partSelectorVisible: false
    });
  },

  // --- Customer Search ---
  onCustomerSearchInput(e: any) {
    this.setData({ customerSearchKeyword: e.detail.value });
  },

  onCustomerSearch() {
    const keyword = this.data.customerSearchKeyword.trim();
    if (!keyword) {
      Toast({ context: this, selector: '#t-toast', message: '请输入客户姓名或手机号', icon: 'close-circle' });
      return;
    }
    searchCustomers(keyword).then(res => {
      this.setData({ customerSearchResults: res.data || [] });
    }).catch(console.error);
  },

  openCustomerSearch() {
    this.setData({
      customerSearchVisible: true,
      customerSearchKeyword: '',
      customerSearchResults: []
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
      'draft.customerNameSnapshot': customer.customerName,
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
    searchVehicles(keyword).then(res => {
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
      'draft.vehicleModelSnapshot': vehicle.model || '',
      'draft.frameNoSnapshot': vehicle.frameNo || '',
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
    if (!order?.canRecordPayment) {
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
