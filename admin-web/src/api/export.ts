import request from '@/utils/request';
import { parseFilenameFromContentDisposition, downloadBlob } from '@/utils/download';
import { setSearchParam } from '@/utils/searchParams';
import { getFriendlyErrorMessage } from '@/utils/statusText';
import type { FinanceQuery } from '@/types/finance';
import type { ReimbursementQuery } from '@/types/reimbursement';
import type { CustomerQuery } from '@/api/customer';
import type { PartViewQuery } from '@/api/parts';

export interface ImportSummary {
  totalRows: number;
  successRows: number;
  message: string;
}

export interface ImportResult {
  success: boolean;
  summary?: ImportSummary;
  errorFileDownloaded?: boolean;
}

export async function handleBlobResponse(response: any, defaultFilename: string) {
  // If the backend returns an error inside 200 via json instead of blob, handle it
  if (response.data && response.data.type && response.data.type.includes('application/json')) {
    const text = await response.data.text();
    let message = '导出失败';
    try {
      const json = JSON.parse(text);
      message = getFriendlyErrorMessage(json.code, json.message || message);
    } catch {
      // Keep the default message when the response is not valid JSON.
    }
    throw new Error(message);
  }

  const filename = parseFilenameFromContentDisposition(response.headers['content-disposition'], defaultFilename);
  downloadBlob(response.data, filename);
}

async function handleImportBlobResponse(response: any, defaultErrorFilename: string): Promise<ImportResult> {
  const blob = response.data as Blob;
  if (blob && blob.type && blob.type.includes('application/json')) {
    const text = await blob.text();
    let json: any;
    try {
      json = JSON.parse(text);
    } catch {
      throw new Error('导入结果解析失败，请刷新后重试');
    }
    if (json?.code === 'SUCCESS') {
      return { success: true, summary: json.data };
    }
    throw new Error(getFriendlyErrorMessage(json?.code, json?.message || '导入失败'));
  }

  const filename = parseFilenameFromContentDisposition(response.headers['content-disposition'], defaultErrorFilename);
  downloadBlob(blob, filename);
  return { success: false, errorFileDownloaded: true };
}

export async function exportFinance(params: FinanceQuery) {
  // Clean up params based on reportType
  const query: Record<string, any> = { reportType: params.reportType === 'CUSTOM' ? 'RANGE' : params.reportType };
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
  setSearchParam(query, 'reimbursementNo', params.reimbursementNo);
  setSearchParam(query, 'applicantName', params.applicantName);
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

export async function exportCustomers(params: CustomerQuery) {
  const query: Record<string, any> = {};
  setSearchParam(query, 'customerName', params.customerName);
  setSearchParam(query, 'phone', params.phone);
  const response = await request.get('/api/admin/exports/customers', {
    params: query,
    responseType: 'blob'
  });
  await handleBlobResponse(response, 'customers.xlsx');
}

export async function exportParts(params: PartViewQuery) {
  const query: Record<string, any> = {};
  setSearchParam(query, 'partCode', params.partCode);
  setSearchParam(query, 'partName', params.partName);
  setSearchParam(query, 'officialPartNo', params.officialCode);
  setSearchParam(query, 'barcode', params.barcode);
  setSearchParam(query, 'model', params.model);
  setSearchParam(query, 'categoryCode', params.category);
  if (params.source) query.source = params.source.toUpperCase();
  if (params.status !== undefined && params.status !== '') {
    query.enabled = params.status === true || params.status === 'true';
  }
  const response = await request.get('/api/admin/exports/parts', {
    params: query,
    responseType: 'blob'
  });
  await handleBlobResponse(response, 'parts.xlsx');
}

export async function downloadCustomerImportTemplate() {
  const response = await request.get('/api/admin/exports/templates/customers', {
    responseType: 'blob'
  });
  await handleBlobResponse(response, '客户车辆导入模板.xlsx');
}

export async function downloadPartImportTemplate() {
  const response = await request.get('/api/admin/exports/templates/parts', {
    responseType: 'blob'
  });
  await handleBlobResponse(response, '配件导入模板.xlsx');
}

export async function importCustomers(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await request.post('/api/admin/exports/imports/customers', formData, {
    responseType: 'blob'
  });
  return handleImportBlobResponse(response, '客户车辆导入错误.xlsx');
}

export async function importParts(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await request.post('/api/admin/exports/imports/parts', formData, {
    responseType: 'blob'
  });
  return handleImportBlobResponse(response, '配件导入错误.xlsx');
}
