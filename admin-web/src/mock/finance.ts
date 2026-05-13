import type { FinanceSummary, FinanceDetailRecord } from '@/types/finance';

export const mockDailySummaries: FinanceDetailRecord[] = [
  {
    id: 'D1',
    dateOrMonth: '2023-10-24',
    customerPaidIncome: 1500,
    officialSettlementIncome: 300,
    partsIncome: 1000,
    laborIncome: 450,
    otherIncome: 50,
    partsCost: 600,
    reimbursementCost: 100,
    profit: 1100
  },
  {
    id: 'D2',
    dateOrMonth: '2023-10-23',
    customerPaidIncome: 800,
    officialSettlementIncome: 0,
    partsIncome: 500,
    laborIncome: 300,
    otherIncome: 0,
    partsCost: 400,
    reimbursementCost: 0,
    profit: 400
  },
  {
    id: 'D3',
    dateOrMonth: '2023-10-22',
    customerPaidIncome: 2100,
    officialSettlementIncome: 500,
    partsIncome: 1200,
    laborIncome: 800,
    otherIncome: 100,
    partsCost: 800,
    reimbursementCost: 50,
    profit: 1750
  },
  {
    id: 'D4',
    dateOrMonth: '2023-10-21',
    customerPaidIncome: 400,
    officialSettlementIncome: 120,
    partsIncome: 200,
    laborIncome: 200,
    otherIncome: 0,
    partsCost: 350,
    reimbursementCost: 100,
    profit: 70
  },
  {
    id: 'D5',
    dateOrMonth: '2023-10-20',
    customerPaidIncome: 1100,
    officialSettlementIncome: 200,
    partsIncome: 700,
    laborIncome: 400,
    otherIncome: 0,
    partsCost: 500,
    reimbursementCost: 0,
    profit: 800
  },
  {
    id: 'D6',
    dateOrMonth: '2023-10-19',
    customerPaidIncome: 3000,
    officialSettlementIncome: 150,
    partsIncome: 2000,
    laborIncome: 800,
    otherIncome: 200,
    partsCost: 1500,
    reimbursementCost: 200,
    profit: 1450
  },
  {
    id: 'D7',
    dateOrMonth: '2023-10-18',
    customerPaidIncome: 900,
    officialSettlementIncome: 0,
    partsIncome: 700,
    laborIncome: 200,
    otherIncome: 0,
    partsCost: 650,
    reimbursementCost: 200,
    profit: 50
  }
];

export const mockMonthlySummaries: FinanceDetailRecord[] = [
  {
    id: 'M1',
    dateOrMonth: '2023-10',
    customerPaidIncome: 45000,
    officialSettlementIncome: 8000,
    partsIncome: 28000,
    laborIncome: 15000,
    otherIncome: 2000,
    partsCost: 18000,
    reimbursementCost: 3000,
    profit: 32000
  },
  {
    id: 'M2',
    dateOrMonth: '2023-09',
    customerPaidIncome: 42000,
    officialSettlementIncome: 7500,
    partsIncome: 25000,
    laborIncome: 15000,
    otherIncome: 2000,
    partsCost: 16000,
    reimbursementCost: 2500,
    profit: 31000
  },
  {
    id: 'M3',
    dateOrMonth: '2023-08',
    customerPaidIncome: 55000,
    officialSettlementIncome: 9000,
    partsIncome: 35000,
    laborIncome: 18000,
    otherIncome: 2000,
    partsCost: 22000,
    reimbursementCost: 4000,
    profit: 38000
  },
  {
    id: 'M4',
    dateOrMonth: '2023-07',
    customerPaidIncome: 48000,
    officialSettlementIncome: 8500,
    partsIncome: 30000,
    laborIncome: 16000,
    otherIncome: 2000,
    partsCost: 19000,
    reimbursementCost: 3500,
    profit: 34000
  },
  {
    id: 'M5',
    dateOrMonth: '2023-06',
    customerPaidIncome: 62000,
    officialSettlementIncome: 12000,
    partsIncome: 40000,
    laborIncome: 20000,
    otherIncome: 2000,
    partsCost: 25000,
    reimbursementCost: 5000,
    profit: 44000
  },
  {
    id: 'M6',
    dateOrMonth: '2023-05',
    customerPaidIncome: 35000,
    officialSettlementIncome: 5000,
    partsIncome: 25000,
    laborIncome: 8000,
    otherIncome: 2000,
    partsCost: 15000,
    reimbursementCost: 2000,
    profit: 23000
  }
];

export const mockTotalSummary: FinanceSummary = {
  customerPaidIncome: 287000,
  officialSettlementIncome: 50000,
  totalIncome: 337000,
  partsIncome: 183000,
  laborIncome: 92000,
  otherIncome: 12000,
  partsCost: 115000,
  reimbursementCost: 20000,
  totalCost: 135000,
  profit: 202000,
  profitMargin: '60.0%'
};
