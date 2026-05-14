export interface ReimbursementRecord {
  id: string | number;
  reimbursementNo: string;
  applicantId: number;
  purpose: string;
  amount: number;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
  remark?: string;
  submittedAt?: string;
  confirmedAmount?: number;
  confirmedBy?: number;
  confirmedAt?: string;
  rejectedBy?: number;
  rejectedAt?: string;
  rejectReason?: string;
  cancelledBy?: number;
  cancelledAt?: string;
  cancelReason?: string;
  costIncluded?: boolean;
}

export interface ReimbursementQuery {
  reimbursementNo?: string;
  applicantId?: number;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  pageNo: number;
  pageSize: number;
}
