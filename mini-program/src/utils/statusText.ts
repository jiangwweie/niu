export function getProgressStatusText(status?: string, text?: string) {
  if (text) return text;
  if (!status) return '未知状态';
  const map: Record<string, string> = {
    DRAFT: '新建中',
    REPAIRING: '维修中',
    REPAIR_DONE: '维修完成',
    DELIVERED: '已交付',
    CANCELLED: '已取消',
  };
  return map[status] || `未知状态（${status}）`;
}

export function getCashierStatusText(status?: string, text?: string) {
  if (text) return text;
  if (!status) return '';
  const map: Record<string, string> = {
    NO_CHARGE: '无需收款',
    UNPAID: '未收款',
    PARTIAL_PAID: '部分收款',
    PAID: '已收齐',
    REFUND_PENDING: '待退款',
    PARTIAL_REFUNDED: '部分退款',
    REFUNDED: '已退清',
  };
  return map[status] || `未知状态（${status}）`;
}

export function getInventoryStatusText(status?: string, text?: string) {
  if (text) return text;
  if (!status) return '';
  const map: Record<string, string> = {
    NOT_RESERVED: '未预占',
    RESERVED: '已预占',
    CONSUMED: '已扣减',
    RELEASED: '已释放',
  };
  return map[status] || `未知状态（${status}）`;
}

export function getNoChargeReasonText(reason?: string) {
  if (!reason) return '';
  const map: Record<string, string> = {
    OFFICIAL_AFTER_SALES: '官方售后',
    OFFICIAL: '官方售后',
    FREE_CHECK: '免费检测',
    BOSS_WAIVER: '老板免单',
    WARRANTY: '质保处理',
    WARRANTY_FREE: '质保处理',
    FIRST_MAINTENANCE_FREE: '免费检测',
    CUSTOMER_OWN_PARTS: '自带配件',
    NO_CHARGE_ITEM: '其他',
    OTHER: '其他',
  };
  return map[reason] || `其他（${reason}）`;
}

export function getFriendlyErrorMessage(code?: string | number, message?: string) {
  const map: Record<string, string> = {
    WORK_ORDER_LEGACY_SETTLE_DISABLED: '状态模型已升级，该工单当前状态无法执行此操作。',
    WORK_ORDER_LEGACY_STATUS_EXISTS: '存在未兼容的业务数据，请联系系统管理员。',
    PAYMENT_EXCEEDS_RECEIVABLE: '收款金额超过待收金额。',
    REFUND_EXCEEDS_PAID_AMOUNT: '退款金额超过可退金额。',
    FORBIDDEN: '当前账号无权操作。',
    UNAUTHORIZED: '登录已过期，请重新登录。',
  };
  const key = code == null ? '' : String(code);
  return map[key] || message || '请求失败';
}
