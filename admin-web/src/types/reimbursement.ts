export interface ReimbursementRecord {
  id: string;
  reimbursementNo: string;
  applicant: string;
  purpose: string;
  amount: number;
  approvedAmount?: number;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
  createdAt: string;
  confirmer?: string;
  confirmedAt?: string;
  remark?: string;
  rejectReason?: string;
  cancelReason?: string;
  processor?: string;
  processedAt?: string;
}

export interface ReimbursementQuery {
  reimbursementNo?: string;
  applicant?: string;
  keyword?: string;
  status?: string;
  dateRange?: [string, string];
  confirmer?: string;
  pageNo: number;
  pageSize: number;
}
