import type { PaginatedResult } from '@/types';

export interface FundingApplication {
  id: number;
  applicationNo: string;
  customerName: string;
  phone: string;
  idCardNo: string;
  vehicleModel: string;
  pickupDate: string;
  paymentType: string;
  purchaseCost: number;
  incentiveAmount: number;
  upstreamAmount: number;
  totalCost: number;
  retailPrice: number;
  receivableAmount: number;
  downPayment: number;
  installmentCount: number;
  installmentAmount: number;
  firstDueDate?: string;
  groupLeader: string;
  handlerName?: string;
  addOnRemark?: string;
  status: string;
  auditRemark?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FundingContract {
  id: number;
  applicationId: number;
  contractNo: string;
  contractType: string;
  signedDate?: string;
  status: string;
  remark?: string;
}

export interface FundingLedger {
  id: number;
  applicationId: number;
  contractId: number;
  ledgerNo: string;
  customerName: string;
  phone: string;
  idCardNo: string;
  vehicleModel: string;
  pickupDate: string;
  paymentType: string;
  purchaseCost: number;
  incentiveAmount: number;
  upstreamAmount: number;
  totalCost: number;
  retailPrice: number;
  receivableAmount: number;
  receivedAmount: number;
  outstandingAmount: number;
  groupLeader: string;
  handlerName?: string;
  status: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FundingInstallmentPlan {
  id: number;
  ledgerId: number;
  phaseNo: number;
  phaseName: string;
  dueDate?: string;
  receivableAmount: number;
  receivedAmount: number;
  status: string;
  remark?: string;
}

export interface FundingPayment {
  id: number;
  ledgerId: number;
  installmentPlanId?: number;
  paymentNo: string;
  amount: number;
  paymentMethod: string;
  paidAt: string;
  remark?: string;
}

export interface FundingAttachment {
  id: number;
  ownerType: string;
  ownerId: number;
  attachmentType: string;
  originalFilename: string;
  contentType?: string;
  fileSize: number;
  remark?: string;
  createdAt?: string;
}

export interface FundingChangeLog {
  id: number;
  ledgerId: number;
  fieldName: string;
  oldValue?: string;
  newValue?: string;
  operatedAt: string;
  remark?: string;
}

export interface FundingDetail {
  application: FundingApplication;
  contract?: FundingContract;
  ledger?: FundingLedger;
  installmentPlans: FundingInstallmentPlan[];
  payments: FundingPayment[];
  attachments: FundingAttachment[];
  changeLogs: FundingChangeLog[];
}

export interface FundingSummary {
  applicationCount: number;
  pendingAuditCount: number;
  ledgerCount: number;
  receivableTotal: number;
  receivedTotal: number;
  outstandingTotal: number;
  overduePlanCount: number;
}

export interface FundingApplicationQuery {
  keyword?: string;
  status?: string;
  pageNo: number;
  pageSize: number;
}

export interface FundingLedgerQuery {
  keyword?: string;
  status?: string;
  pageNo: number;
  pageSize: number;
}

export interface SaveFundingApplicationBody {
  customerName: string;
  phone: string;
  idCardNo: string;
  vehicleModel: string;
  pickupDate: string;
  paymentType: string;
  purchaseCost?: number;
  incentiveAmount?: number;
  upstreamAmount?: number;
  totalCost?: number;
  retailPrice?: number;
  receivableAmount: number;
  downPayment?: number;
  installmentCount?: number;
  installmentAmount?: number;
  firstDueDate?: string;
  groupLeader: string;
  handlerName?: string;
  addOnRemark?: string;
  remark?: string;
}

export type FundingPage<T> = PaginatedResult<T>;
