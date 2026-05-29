const LEGACY_WORK_ORDER_STATUSES = new Set([
  'PENDING_ACCEPT',
  'ACCEPTED',
  'PART_ORDERED',
  'PART_ARRIVED',
  'SETTLED',
]);

export const LEGACY_WORK_ORDER_STATUS_TEXT = '旧状态，请先清理试运行数据';

const PROGRESS_STATUS_MAP: Record<string, string> = {
  DRAFT: '新建中',
  REPAIRING: '维修中',
  REPAIR_DONE: '维修完成',
  DELIVERED: '已交付',
  CANCELLED: '已取消',
  // Legacy statuses — kept for display so old data is readable
  PENDING_ACCEPT: '待接单',
  ACCEPTED: '已接单',
  PART_ORDERED: '配件已订',
  PART_ARRIVED: '配件已到',
  SETTLED: '已结算',
};

export const getProgressStatusText = (status?: string | null, text?: string | null) => {
  if (!status) return '-';
  // Frontend map takes priority to ensure consistent Chinese display
  if (PROGRESS_STATUS_MAP[status]) return PROGRESS_STATUS_MAP[status];
  // Fall back to backend-provided text for any future statuses
  if (text) return text;
  return '-';
};

export const getCashierStatusText = (status?: string | null, text?: string | null) => {
  if (text) return text;
  if (!status) return '-';
  const map: Record<string, string> = {
    NO_CHARGE: '无需收款',
    UNPAID: '未收款',
    PARTIAL_PAID: '部分收款',
    PAID: '已收齐',
    REFUND_PENDING: '待退款',
    PARTIAL_REFUNDED: '部分退款',
    REFUNDED: '已退清',
  };
  return map[status] || '-';
};

export const getInventoryStatusText = (status?: string | null, text?: string | null) => {
  if (text) return text;
  if (!status) return '-';
  const map: Record<string, string> = {
    NOT_RESERVED: '未预占',
    RESERVED: '已预占',
    CONSUMED: '已扣减',
    RELEASED: '已释放',
  };
  return map[status] || '-';
};

export const getNoChargeReasonText = (reason?: string | null) => {
  if (!reason) return '-';
  const map: Record<string, string> = {
    OFFICIAL_AFTER_SALES: '官方售后',
    OFFICIAL: '官方售后',
    FREE_CHECK: '免费检测',
    BOSS_WAIVER: '老板免单',
    WARRANTY: '质保处理',
    WARRANTY_FREE: '质保处理',
    FIRST_MAINTENANCE_FREE: '免费检测',
    CUSTOMER_OWN_PARTS: '其他',
    NO_CHARGE_ITEM: '其他',
    OTHER: '其他',
  };
  return map[reason] || '其他';
};

export const getFriendlyErrorMessage = (code?: string, message?: string) => {
  const map: Record<string, string> = {
    WORK_ORDER_LEGACY_SETTLE_DISABLED: '状态模型已升级，请使用交付关闭。',
    WORK_ORDER_LEGACY_STATUS_EXISTS: '存在旧状态工单，请先清理试运行数据。',
    PAYMENT_EXCEEDS_RECEIVABLE: '收款金额超过待收金额。',
    REFUND_EXCEEDS_PAID_AMOUNT: '退款金额超过可退金额。',
    FORBIDDEN: '当前账号无权操作。',
    UNAUTHORIZED: '登录已过期，请重新登录。',
  };
  return (code && map[code]) || message || '请求失败';
};
