import { request } from '../utils/request';
import { SubmitReimbursementRequest, StaffReimbursementResponse } from '../types/reimbursement';

export const submitReimbursement = (data: SubmitReimbursementRequest) => {
  return request<StaffReimbursementResponse>({
    url: '/api/staff/reimbursements',
    method: 'POST',
    data,
    mockData: {
      id: Date.now(),
      purpose: data.purpose,
      amount: data.amount,
      status: 'PENDING',
      remark: data.remark,
      submittedAt: new Date().toISOString(),
      costIncluded: false
    } as StaffReimbursementResponse,
    showLoading: true
  });
};
