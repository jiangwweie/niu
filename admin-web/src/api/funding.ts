import request from '@/utils/request';
import { handleBlobResponse } from '@/api/export';
import { setSearchParam } from '@/utils/searchParams';
import type {
  FundingApplication,
  FundingApplicationQuery,
  FundingDetail,
  FundingLedger,
  FundingLedgerQuery,
  FundingPayment,
  FundingSummary,
  SaveFundingApplicationBody,
  FundingPage,
} from '@/types/funding';

export async function getFundingSummary(): Promise<FundingSummary> {
  return await request.get('/api/admin/funding/summary');
}

export async function getFundingApplications(params: FundingApplicationQuery): Promise<FundingPage<FundingApplication>> {
  const query: Record<string, unknown> = { pageNo: params.pageNo, pageSize: params.pageSize };
  setSearchParam(query, 'keyword', params.keyword);
  if (params.status) query.status = params.status;
  return await request.get('/api/admin/funding/applications', { params: query });
}

export async function createFundingApplication(data: SaveFundingApplicationBody): Promise<FundingApplication> {
  return await request.post('/api/admin/funding/applications', data);
}

export async function updateFundingApplication(id: number, data: SaveFundingApplicationBody): Promise<FundingApplication> {
  return await request.put(`/api/admin/funding/applications/${id}`, data);
}

export async function getFundingApplicationDetail(id: number): Promise<FundingDetail> {
  return await request.get(`/api/admin/funding/applications/${id}`);
}

export async function submitFundingApplication(id: number): Promise<FundingApplication> {
  return await request.post(`/api/admin/funding/applications/${id}/submit`);
}

export async function approveFundingApplication(id: number): Promise<FundingApplication> {
  return await request.post(`/api/admin/funding/applications/${id}/approve`);
}

export async function rejectFundingApplication(id: number, reason: string): Promise<FundingApplication> {
  return await request.post(`/api/admin/funding/applications/${id}/reject`, { reason });
}

export async function saveFundingContract(applicationId: number, data: { contractNo?: string; contractType: string; signedDate?: string; remark?: string }) {
  return await request.post(`/api/admin/funding/applications/${applicationId}/contract`, data);
}

export async function confirmFundingContract(contractId: number): Promise<FundingLedger> {
  return await request.post(`/api/admin/funding/contracts/${contractId}/confirm`);
}

export async function getFundingLedgers(params: FundingLedgerQuery): Promise<FundingPage<FundingLedger>> {
  const query: Record<string, unknown> = { pageNo: params.pageNo, pageSize: params.pageSize };
  setSearchParam(query, 'keyword', params.keyword);
  if (params.status) query.status = params.status;
  return await request.get('/api/admin/funding/ledgers', { params: query });
}

export async function getFundingLedgerDetail(id: number): Promise<FundingDetail> {
  return await request.get(`/api/admin/funding/ledgers/${id}`);
}

export async function updateFundingLedger(id: number, data: Partial<FundingLedger> & { changeRemark?: string }): Promise<FundingLedger> {
  return await request.put(`/api/admin/funding/ledgers/${id}`, data);
}

export async function recordFundingPayment(id: number, data: { installmentPlanId?: number; amount: number; paymentMethod: string; paidAt?: string; remark?: string }): Promise<FundingPayment> {
  return await request.post(`/api/admin/funding/ledgers/${id}/payments`, data);
}

export async function uploadFundingAttachment(data: FormData) {
  return await request.post('/api/admin/funding/attachments', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export async function downloadFundingAttachment(id: number, filename?: string) {
  const response = await request.get(`/api/admin/funding/attachments/${id}/download`, {
    responseType: 'blob'
  });
  await handleBlobResponse(response, filename || 'funding_attachment');
}
