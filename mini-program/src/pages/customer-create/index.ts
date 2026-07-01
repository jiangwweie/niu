import { createCustomer, createVehicle } from '../../api/customer';
import Toast from 'tdesign-miniprogram/toast/index';
import { requireAnyPermission, requireLogin } from '../../utils/permission';
import { showRequestErrorToast } from '../../utils/requestError';

type CustomerForm = {
  customerName: string;
  phone: string;
  remark: string;
};

type VehicleForm = {
  model: string;
  frameNo: string;
  batteryNo: string;
  remark: string;
};

const emptyCustomerForm = (): CustomerForm => ({
  customerName: '',
  phone: '',
  remark: ''
});

const emptyVehicleForm = (): VehicleForm => ({
  model: '',
  frameNo: '',
  batteryNo: '',
  remark: ''
});

const trimValue = (value?: string) => (value || '').trim();

Page({
  data: {
    customerForm: emptyCustomerForm(),
    vehicleForm: emptyVehicleForm(),
    submitLoading: false,
    createdCustomerId: null as number | null,
    successMessage: ''
  },

  onShow() {
    if (!requireLogin('/pages/customer-create/index')) return;
    if (!requireAnyPermission(['CUSTOMER_MANAGE'], '当前账号无权录入客户')) return;
  },

  onCustomerFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`customerForm.${field}`]: e.detail.value,
      successMessage: ''
    });
  },

  onVehicleFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`vehicleForm.${field}`]: e.detail.value,
      successMessage: ''
    });
  },

  hasVehicleInput(): boolean {
    const vehicle = this.data.vehicleForm;
    return Boolean(
      trimValue(vehicle.model)
      || trimValue(vehicle.frameNo)
      || trimValue(vehicle.batteryNo)
      || trimValue(vehicle.remark)
    );
  },

  validateForm(): boolean {
    const customer = this.data.customerForm;
    const vehicle = this.data.vehicleForm;
    const customerName = trimValue(customer.customerName);
    const phone = trimValue(customer.phone);

    if (!customerName) {
      Toast({ context: this, selector: '#t-toast', message: '请输入客户姓名', icon: 'close-circle' });
      return false;
    }

    if (customerName.length > 64) {
      Toast({ context: this, selector: '#t-toast', message: '客户姓名不能超过64个字', icon: 'close-circle' });
      return false;
    }

    if (phone && phone.length > 32) {
      Toast({ context: this, selector: '#t-toast', message: '手机号不能超过32个字符', icon: 'close-circle' });
      return false;
    }

    if (phone && !/^[0-9+\-\s]{6,32}$/.test(phone)) {
      Toast({ context: this, selector: '#t-toast', message: '手机号格式不正确，请检查后重试', icon: 'close-circle' });
      return false;
    }

    if (this.hasVehicleInput() && !trimValue(vehicle.frameNo)) {
      Toast({ context: this, selector: '#t-toast', message: '登记车辆时必须填写车架号', icon: 'close-circle' });
      return false;
    }

    if (trimValue(vehicle.frameNo).length > 128
        || trimValue(vehicle.model).length > 128
        || trimValue(vehicle.batteryNo).length > 128) {
      Toast({ context: this, selector: '#t-toast', message: '车辆字段不能超过128个字', icon: 'close-circle' });
      return false;
    }

    return true;
  },

  onSubmit() {
    if (this.data.submitLoading) return;
    if (!this.validateForm()) return;

    const customer = this.data.customerForm;
    const vehicle = this.data.vehicleForm;
    const shouldCreateVehicle = this.hasVehicleInput();

    this.setData({ submitLoading: true, successMessage: '' });

    createCustomer({
      customerName: trimValue(customer.customerName),
      phone: trimValue(customer.phone) || undefined,
      remark: trimValue(customer.remark) || undefined
    }).then((res) => {
      const customerId = res.data;
      if (!shouldCreateVehicle) {
        this.setData({
          createdCustomerId: customerId,
          successMessage: '客户已保存，可继续新建工单。',
          customerForm: emptyCustomerForm(),
          vehicleForm: emptyVehicleForm()
        });
        Toast({ context: this, selector: '#t-toast', message: '客户已保存', icon: 'check-circle' });
        return;
      }

      return createVehicle(customerId, {
        frameNo: trimValue(vehicle.frameNo),
        model: trimValue(vehicle.model) || undefined,
        batteryNo: trimValue(vehicle.batteryNo) || undefined,
        remark: trimValue(vehicle.remark) || undefined
      }).then(() => {
        this.setData({
          createdCustomerId: customerId,
          successMessage: '客户和车辆已保存，可继续新建工单。',
          customerForm: emptyCustomerForm(),
          vehicleForm: emptyVehicleForm()
        });
        Toast({ context: this, selector: '#t-toast', message: '客户和车辆已保存', icon: 'check-circle' });
      }).catch((err: any) => {
        console.error('Create vehicle failed after customer saved:', err);
        this.setData({
          createdCustomerId: customerId,
          successMessage: '客户已保存，车辆登记失败，可稍后在工单中补录车辆信息。'
        });
        showRequestErrorToast(err, '客户已保存，车辆登记失败，请检查车架号后重试');
      });
    }).catch((err: any) => {
      console.error('Create customer failed:', err);
      showRequestErrorToast(err, '客户保存失败，请检查姓名和手机号后重试');
    }).finally(() => {
      this.setData({ submitLoading: false });
    });
  },

  onContinueCreate() {
    this.setData({
      customerForm: emptyCustomerForm(),
      vehicleForm: emptyVehicleForm(),
      createdCustomerId: null,
      successMessage: ''
    });
  },

  goToCreateOrder() {
    wx.navigateTo({ url: '/pages/create-work-order/index' });
  },

  backToDashboard() {
    wx.switchTab({ url: '/pages/dashboard/index' });
  }
});
