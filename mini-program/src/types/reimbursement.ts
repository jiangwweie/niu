export interface SubmitReimbursementRequest {
  purpose: string;
  amount: number;
  remark?: string;
}

export interface StaffReimbursementResponse {
  id: number | string;
  storeId?: number | string;
  applicantId?: number | string;
  purpose: string;
  amount: number;
  status: string;
  remark?: string;
  submittedAt?: string;
  costIncluded?: boolean;
}
