export interface FundingApplication {
  id: number;
  applicationNo: string;
  customerName: string;
  phone: string;
  idCardNo: string;
  vehicleModel: string;
  pickupDate: string;
  paymentType: string;
  receivableAmount: number;
  downPayment: number;
  installmentCount: number;
  installmentAmount: number;
  firstDueDate?: string;
  groupLeader: string;
  status: string;
}

export interface FundingLedger {
  id: number;
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
}

export interface FundingInstallmentPlan {
  id: number;
  phaseName: string;
  dueDate?: string;
  receivableAmount: number;
  receivedAmount: number;
  status: string;
}

export interface FundingDetail {
  application?: FundingApplication;
  ledger?: FundingLedger;
  installmentPlans: FundingInstallmentPlan[];
  attachments?: FundingAttachment[];
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

export interface SaveFundingApplicationRequest {
  customerName: string;
  phone: string;
  idCardNo: string;
  vehicleModel: string;
  pickupDate: string;
  paymentType: string;
  receivableAmount: number;
  downPayment?: number;
  installmentCount?: number;
  installmentAmount?: number;
  firstDueDate?: string;
  groupLeader: string;
  purchaseCost?: number;
  incentiveAmount?: number;
  upstreamAmount?: number;
  totalCost?: number;
  retailPrice?: number;
}
