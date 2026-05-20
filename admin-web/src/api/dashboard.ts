import request from '@/utils/request';
import type { DashboardSummaryResponse } from '@/types/dashboard';

export async function getDashboardSummary(): Promise<DashboardSummaryResponse> {
  return await request.get('/api/admin/dashboard/summary');
}
