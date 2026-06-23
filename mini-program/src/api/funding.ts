import { request } from '../utils/request';
import { API_MODE, getBaseUrl } from '../utils/config';
import { authStore } from '../stores/auth';
import { PageResponse } from '../types/common';
import { FundingApplication, FundingDetail, FundingLedger, SaveFundingApplicationRequest } from '../types/funding';

export const getFundingApplications = (params?: any) => {
  return request<PageResponse<FundingApplication>>({
    url: '/api/staff/funding/applications',
    method: 'GET',
    data: params,
    showLoading: true
  });
};

export const createFundingApplication = (data: SaveFundingApplicationRequest) => {
  return request<FundingApplication>({
    url: '/api/staff/funding/applications',
    method: 'POST',
    data,
    showLoading: true
  });
};

export const submitFundingApplication = (id: number) => {
  return request<FundingApplication>({
    url: `/api/staff/funding/applications/${id}/submit`,
    method: 'POST',
    showLoading: true
  });
};

export const getFundingApplicationDetail = (id: number) => {
  return request<FundingDetail>({
    url: `/api/staff/funding/applications/${id}`,
    method: 'GET',
    showLoading: true
  });
};

export const getFundingLedgers = (params?: any) => {
  return request<PageResponse<FundingLedger>>({
    url: '/api/staff/funding/ledgers',
    method: 'GET',
    data: params,
    showLoading: true
  });
};

export const getFundingLedgerDetail = (id: number) => {
  return request<FundingDetail>({
    url: `/api/staff/funding/ledgers/${id}`,
    method: 'GET',
    showLoading: true
  });
};

export const updateFundingLedger = (id: number, data: Partial<FundingLedger> & { changeRemark?: string }) => {
  return request<FundingLedger>({
    url: `/api/staff/funding/ledgers/${id}`,
    method: 'PUT',
    data,
    showLoading: true
  });
};

export const recordFundingPayment = (ledgerId: number, data: { installmentPlanId?: number; amount: number; paymentMethod: string; paidAt?: string; remark?: string }) => {
  return request<any>({
    url: `/api/staff/funding/ledgers/${ledgerId}/payments`,
    method: 'POST',
    data,
    showLoading: true
  });
};

export const uploadFundingAttachment = (data: { ownerType: string; ownerId: number; attachmentType: string; filePath: string; remark?: string }) => {
  if (API_MODE === 'mock') {
    return Promise.resolve({
      code: 'SUCCESS',
      message: 'success',
      data: {},
      traceId: `mock_trace_${Date.now()}`
    });
  }

  return new Promise<any>((resolve, reject) => {
    wx.showLoading({ title: '上传中...', mask: true });
    wx.uploadFile({
      url: `${getBaseUrl()}/api/staff/funding/attachments`,
      filePath: data.filePath,
      name: 'file',
      header: authStore.accessToken ? { Authorization: `Bearer ${authStore.accessToken}` } : {},
      formData: {
        ownerType: data.ownerType,
        ownerId: String(data.ownerId),
        attachmentType: data.attachmentType,
        remark: data.remark || ''
      },
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error('附件上传失败'));
          return;
        }
        try {
          const body = JSON.parse(res.data);
          if (body.code !== 'SUCCESS' && body.code !== 200) {
            reject(new Error(body.message || '附件上传失败'));
            return;
          }
          resolve(body);
        } catch {
          reject(new Error('附件上传响应异常'));
        }
      },
      fail: reject,
      complete: () => wx.hideLoading()
    });
  });
};

export const downloadFundingAttachment = (id: number, filename?: string) => {
  if (API_MODE === 'mock') {
    return Promise.resolve();
  }
  return new Promise<void>((resolve, reject) => {
    wx.showLoading({ title: '打开中...', mask: true });
    wx.downloadFile({
      url: `${getBaseUrl()}/api/staff/funding/attachments/${id}/download`,
      header: authStore.accessToken ? { Authorization: `Bearer ${authStore.accessToken}` } : {},
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error('附件下载失败'));
          return;
        }
        const ext = filename?.split('.').pop()?.toLowerCase();
        const fileType = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf'].includes(ext || '')
          ? ext as 'doc' | 'docx' | 'xls' | 'xlsx' | 'ppt' | 'pptx' | 'pdf'
          : undefined;
        if (!fileType) {
          wx.showToast({ title: '文件已下载，当前格式不支持预览', icon: 'none' });
          resolve();
          return;
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType,
          showMenu: true,
          success: () => resolve(),
          fail: reject
        });
      },
      fail: reject,
      complete: () => wx.hideLoading()
    });
  });
};
