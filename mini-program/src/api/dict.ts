import { request } from '../utils/request';

export const getDictItems = (typeCode: string) => {
  return request<any[]>({
    url: `/api/staff/dict/types/${typeCode}/items`,
    method: 'GET',
    mockData: []
  });
};
