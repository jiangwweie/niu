import request from '@/utils/request';
import type { ReimbursementQuery, ReimbursementRecord } from '@/types/reimbursement';

export async function getReimbursementList(params: ReimbursementQuery): Promise<{ records: ReimbursementRecord[]; total: number }> {
  const page: any = await request.get('/api/admin/reimbursements', { params });
  return {
    records: page.records || [],
    total: page.total || 0,
  };
}

export async function getReimbursementDetail(id: string | number): Promise<ReimbursementRecord> {
  return await request.get(`/api/admin/reimbursements/${id}`);
}

export async function confirmReimbursement(id: string | number, data: { confirmedAmount: number; remark?: string }): Promise<ReimbursementRecord> {
  return await request.post(`/api/admin/reimbursements/${id}/confirm`, data);
}

export async function rejectReimbursement(id: string | number, data: { rejectReason: string }): Promise<ReimbursementRecord> {
  return await request.post(`/api/admin/reimbursements/${id}/reject`, data);
}
