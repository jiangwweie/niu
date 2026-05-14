import { getParts } from '../../api/parts';
import { 
  createDraftWorkOrder, 
  getWorkOrderDetail, 
  addChargeItem, 
  updateChargeItem, 
  deleteChargeItem 
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

Page({
  data: {
    step: 1, // 1: Draft Base Info, 2: Charge Items
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

    // Settle
    settleDialogVisible: false,
    settleRemark: '',
    settleLoading: false
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

    const req: CreateDraftWorkOrderRequest = { ...this.data.draft };

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
      this.setData({ workOrder: res.data });
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
      selectedPart: null, // Edit doesn't need to reselect part in this MVP usually
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
      // Update
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
      // Add
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
        Toast({ context: this, selector: '#t-toast', message: '费用明细已添加。DRAFT 阶段不会预占库存。', icon: 'check-circle' });
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
        Toast({ context: this, selector: '#t-toast', message: '费用明细已删除。DRAFT 阶段不会生成库存流水。', icon: 'check-circle' });
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

    import('../../api/workOrder').then(({ submitWorkOrder }) => {
      submitWorkOrder(this.data.workOrderId!, { remark: this.data.submitRemark }).then(res => {
        Toast({ context: this, selector: '#t-toast', message: '工单已提交，库存预占以后端结果为准。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(err => {
        // Backend error messages will be shown by request wrapper (Toast)
        console.error('Submit work order failed:', err);
      }).finally(() => {
        this.setData({ submitLoading: false });
      });
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

    import('../../api/workOrder').then(({ cancelWorkOrder }) => {
      cancelWorkOrder(this.data.workOrderId!, { 
        reason: this.data.cancelReason, 
        remark: this.data.cancelRemark 
      }).then(res => {
        Toast({ context: this, selector: '#t-toast', message: '工单已取消，库存释放以后端结果为准。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(err => {
        console.error('Cancel work order failed:', err);
      }).finally(() => {
        this.setData({ cancelLoading: false });
      });
    });
  },

  // --- Record Payment ---
  openPaymentDialog() {
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
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的支付金额', icon: 'close-circle' });
      return;
    }

    if (!this.data.paymentForm.paymentMethod) {
      Toast({ context: this, selector: '#t-toast', message: '请选择支付方式', icon: 'close-circle' });
      return;
    }

    this.setData({ paymentLoading: true });

    import('../../api/workOrder').then(({ recordPayment }) => {
      recordPayment(this.data.workOrderId!, {
        amount: amount,
        paymentMethod: this.data.paymentForm.paymentMethod,
        remark: this.data.paymentForm.remark
      }).then(res => {
        this.setData({ paymentDialogVisible: false });
        Toast({ context: this, selector: '#t-toast', message: '支付记录已保存。支付不会自动结算工单。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(err => {
        console.error('Record payment failed:', err);
      }).finally(() => {
        this.setData({ paymentLoading: false });
      });
    });
  },

  // --- Record Refund ---
  openRefundDialog() {
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

    import('../../api/workOrder').then(({ recordRefund }) => {
      recordRefund(this.data.workOrderId!, {
        amount: amount,
        refundMethod: this.data.refundForm.refundMethod,
        reason: this.data.refundForm.reason,
        remark: this.data.refundForm.remark
      }).then(res => {
        this.setData({ refundDialogVisible: false });
        Toast({ context: this, selector: '#t-toast', message: '退款记录已保存。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(err => {
        console.error('Record refund failed:', err);
      }).finally(() => {
        this.setData({ refundLoading: false });
      });
    });
  },

  // --- Settle Work Order ---
  openSettleDialog() {
    this.setData({
      settleDialogVisible: true,
      settleRemark: ''
    });
  },

  onCancelSettleDialog() {
    this.setData({ settleDialogVisible: false });
  },

  onSettleRemarkChange(e: any) {
    this.setData({ settleRemark: e.detail.value });
  },

  onConfirmSettle() {
    if (this.data.settleLoading || !this.data.workOrderId) return;
    
    this.setData({ settleLoading: true, settleDialogVisible: false });

    import('../../api/workOrder').then(({ settleWorkOrder }) => {
      settleWorkOrder(this.data.workOrderId!, { remark: this.data.settleRemark }).then(res => {
        Toast({ context: this, selector: '#t-toast', message: '结算成功。', icon: 'check-circle' });
        this.refreshWorkOrder();
      }).catch(err => {
        console.error('Settle work order failed:', err);
      }).finally(() => {
        this.setData({ settleLoading: false });
      });
    });
  }
});
