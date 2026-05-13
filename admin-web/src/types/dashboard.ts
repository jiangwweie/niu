export interface DashboardStats {
  todayOrders: number;
  todayCustomerIncome: number;
  todayOfficialIncome: number;
  pendingReimbursement: number;
  inventoryWarnings: number;
  unsettledOrders: number;
}

export interface IncomeSplit {
  customerIncome: {
    parts: number;
    labor: number;
    other: number;
  };
  officialIncome: {
    amount: number;
  };
}

export interface TodoReminder {
  id: string;
  type: string;
  content: string;
  count: number;
}

export interface RecentWorkOrder {
  id: string;
  orderNo: string;
  customerName: string;
  scooterModel: string;
  status: string;
  receivableAmount: number;
  actualAmount: number;
  isOfficial: boolean;
  createdAt: string;
}

export interface InventoryWarning {
  id: string;
  partCode: string;
  partName: string;
  availableStock: number;
  warningThreshold: number;
  location: string;
}

export interface DashboardData {
  stats: DashboardStats;
  incomeSplit: IncomeSplit;
  todos: TodoReminder[];
  recentOrders: RecentWorkOrder[];
  inventoryWarnings: InventoryWarning[];
}
