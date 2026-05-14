import { submitReimbursement } from '../../api/reimbursement';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
  data: {
    form: {
      purpose: '',
      amount: '',
      remark: ''
    },
    submitLoading: false
  },

  onFormChange(e: any) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: e.detail.value
    });
  },

  onSubmit() {
    if (this.data.submitLoading) return;

    const { purpose, amount, remark } = this.data.form;

    if (!purpose || purpose.trim() === '') {
      Toast({ context: this, selector: '#t-toast', message: '请输入报销用途', icon: 'close-circle' });
      return;
    }

    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      Toast({ context: this, selector: '#t-toast', message: '请输入大于0的报销金额', icon: 'close-circle' });
      return;
    }

    this.setData({ submitLoading: true });

    submitReimbursement({
      purpose,
      amount: numAmount,
      remark: remark || undefined
    }).then(() => {
      Toast({ context: this, selector: '#t-toast', message: '提交成功，请等待确认。', icon: 'check-circle' });
      this.setData({
        form: { purpose: '', amount: '', remark: '' }
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch((err: any) => {
      console.error('Submit reimbursement failed:', err);
    }).finally(() => {
      this.setData({ submitLoading: false });
    });
  }
});
