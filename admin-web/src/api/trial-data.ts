import request from '@/utils/request';
import type { TrialDataSummaryResponse, ClearTrialDataResponse } from '@/types/trial-data';

export async function getTrialDataSummary(): Promise<TrialDataSummaryResponse> {
  return await request.get('/api/admin/trial-data/summary');
}

export async function clearTrialData(confirmText: string): Promise<ClearTrialDataResponse> {
  return await request.post('/api/admin/trial-data/clear', { confirmText });
}
