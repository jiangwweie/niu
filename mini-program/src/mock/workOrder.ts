import { WorkOrder } from '../types/workOrder';

export const mockWorkOrders = {
  records: [
    {
      id: 405,
      workOrderNo: 'WO202605140001',
      status: 'SUBMITTED',
      customerNameSnapshot: '王先生',
      customerPhoneSnapshot: '13800138000',
      vehicleModelSnapshot: 'NQi GT',
      frameNoSnapshot: 'VIN1234567890',
      batteryNoSnapshot: 'BAT0987654321',
      repairItem: '更换前刹车片、常规保养',
      remark: '客户自带头盔放车厢内',
      receivableAmount: 155.00,
      receivedAmount: 0.00,
      createdAt: '2026-05-14T08:00:00Z',
      chargeItems: [
        {
          id: 1,
          workOrderId: 405,
          chargeType: 'PART',
          itemName: '小牛原装前刹车片 (N系列)',
          partId: 100,
          partCodeSnapshot: 'P-NIU-001',
          quantity: 1,
          unitPrice: 85.00,
          lineAmount: 85.00
        },
        {
          id: 2,
          workOrderId: 405,
          chargeType: 'LABOR',
          itemName: '更换刹车片工时费',
          quantity: 1,
          unitPrice: 50.00,
          lineAmount: 50.00
        },
        {
          id: 3,
          workOrderId: 405,
          chargeType: 'OTHER',
          itemName: '环保处理费',
          quantity: 1,
          unitPrice: 20.00,
          lineAmount: 20.00
        }
      ]
    },
    {
      id: 406,
      workOrderNo: 'WO202605130002',
      status: 'SETTLED',
      customerNameSnapshot: '李女士',
      vehicleModelSnapshot: 'MQi+',
      repairItem: '更换后视镜',
      receivableAmount: 60.00,
      receivedAmount: 60.00,
      createdAt: '2026-05-13T15:00:00Z'
    }
  ] as WorkOrder[],
  pageNo: 1,
  pageSize: 10,
  total: 2
};
