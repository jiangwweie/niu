import request from '@/utils/request';
import { parseFilenameFromContentDisposition, downloadBlob } from '@/utils/download';
import type { FinanceQuery } from '@/types/finance';
import type { ReimbursementQuery } from '@/types/reimbursement';

export async function handleBlobResponse(response: any, defaultFilename: string) {
  // If the backend returns an error inside 200 via json instead of blob, handle it
  if (response.data && response.data.type && response.data.type.includes('application/json')) {
    const text = await response.data.text();
    try {
      const json = JSON.parse(text);
      throw new Error(json.message || '导出失败');
    } catch {
      throw new Error('导出失败');
    }
  }

  const filename = parseFilenameFromContentDisposition(response.headers['content-disposition'], defaultFilename);
  downloadBlob(response.data, filename);
}

export async function exportFinance(params: FinanceQuery) {
  // Clean up params based on reportType
  const query: Record<string, any> = { reportType: params.reportType };
  if (params.reportType === 'DAILY') query.date = params.date;
  if (params.reportType === 'MONTHLY') query.month = `${params.year}-${String(params.month).padStart(2, '0')}`;
  if (params.reportType === 'CUSTOM' && params.dateRange) {
    query.startDate = params.dateRange[0];
    query.endDate = params.dateRange[1];
  }

  const response = await request.get('/api/admin/exports/finance', {
    params: query,
    responseType: 'blob'
  });
  await handleBlobResponse(response, 'finance_report.xlsx');
}

export async function exportReimbursements(params: ReimbursementQuery) {
  const query: Record<string, any> = {};
  if (params.status) query.status = params.status;
  if (params.applicantId) query.applicantId = params.applicantId;
  if (params.dateFrom) query.dateFrom = params.dateFrom;
  if (params.dateTo) query.dateTo = params.dateTo;

  const response = await request.get('/api/admin/exports/reimbursements', {
    params: query,
    responseType: 'blob'
  });
  await handleBlobResponse(response, 'reimbursements.xlsx');
}
