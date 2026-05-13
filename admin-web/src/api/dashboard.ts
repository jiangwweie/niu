import type { DashboardData } from '@/types';
import type { BaseHttpResponse } from '@/types';
import { mockDashboardData } from '@/mock/dashboard';

/**
 * 获取首页核心指标看板数据
 */
export const getDashboardStats = (): Promise<BaseHttpResponse<DashboardData>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: mockDashboardData
      });
    }, 300);
  });
};
