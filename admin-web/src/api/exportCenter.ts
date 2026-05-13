import type { BaseHttpResponse, PaginatedResult } from '@/types';
import type { ExportTask, ExportTaskQuery, ExportTypeInfo } from '@/types/exportCenter';
import { mockExportTypes, mockExportTasks } from '@/mock/exportCenter';

export const getExportTypes = (): Promise<BaseHttpResponse<ExportTypeInfo[]>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: [...mockExportTypes]
      });
    }, 300);
  });
};

export const getExportTasks = (params: ExportTaskQuery): Promise<BaseHttpResponse<PaginatedResult<ExportTask>>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockExportTasks];
      
      if (params.typeCode) filtered = filtered.filter(p => p.typeCode === params.typeCode);
      if (params.status) filtered = filtered.filter(p => p.status === params.status);
      
      const pageNo = params.pageNo || 1;
      const pageSize = params.pageSize || 10;
      const start = (pageNo - 1) * pageSize;
      const pagedData = filtered.slice(start, start + pageSize);

      resolve({
        code: 'SUCCESS',
        message: 'success',
        data: {
          records: pagedData,
          total: filtered.length,
          pageNo,
          pageSize
        }
      });
    }, 300);
  });
};
