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
    LOGIN_BAD_CREDENTIALS: '账号或密码不正确，请检查后重试。',
    COMMON_BAD_REQUEST: '提交内容有误，请检查页面提示后重试。',
    COMMON_NOT_FOUND: '数据不存在或已被处理，请返回列表刷新。',
    COMMON_INTERNAL_ERROR: '系统暂时异常，请稍后重试。',
    WORK_ORDER_NOT_FOUND: '工单不存在或已被删除，请返回列表刷新。',
    WORK_ORDER_NOT_DRAFT: '该工单已不是草稿，不能继续编辑。',
    WORK_ORDER_CANCEL_NOT_ALLOWED: '当前状态不能取消工单，请返回详情查看最新状态。',
    WORK_ORDER_CANCEL_REASON_REQUIRED: '请输入取消原因。',
    WORK_ORDER_REPAIR_DONE_NOT_ALLOWED: '当前状态不能标记维修完成，请先确认工单已提交并处于维修中。',
    WORK_ORDER_DELIVER_NOT_ALLOWED: '当前状态不能交付关闭，请先完成维修并收齐尾款。',
    WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH: '实收金额不足，请先记录收款后再交付关闭。',
    WORK_ORDER_NO_CHARGE_REASON_REQUIRED: '应收为 0 时，请选择无需收款原因。',
    WORK_ORDER_NON_INVENTORY_CHARGE_NOT_ALLOWED: '当前状态不能追加费用，请返回详情查看最新状态。',
    WORK_ORDER_LEGACY_SETTLE_DISABLED: '状态模型已升级，请使用“维修完成/交付关闭”流程。',
    WORK_ORDER_LEGACY_STATUS_EXISTS: '存在旧试运行数据，请联系管理员处理。',
    PART_NOT_FOUND: '配件不存在或已被删除，请返回列表刷新。',
    PART_DISABLED: '该配件已停用，请更换配件或联系管理员启用。',
    PART_STOCK_NOT_FOUND: '该配件暂无库存记录，请先完成入库。',
    INVENTORY_AVAILABLE_NOT_ENOUGH: '可用库存不足，请先入库或调整配件库存。',
    INVENTORY_RESERVED_NOT_ENOUGH: '预占库存不足，请刷新工单后重试。',
    PAYMENT_AMOUNT_INVALID: '请输入大于 0 的收款金额。',
    PAYMENT_METHOD_INVALID: '请选择收款方式。',
    PAYMENT_WORK_ORDER_STATUS_INVALID: '当前工单状态不能记录收款。',
    PAYMENT_EXCEEDS_RECEIVABLE: '收款金额超过待收金额，请修改金额后重试。',
    REFUND_AMOUNT_INVALID: '请输入大于 0 的退款金额。',
    REFUND_METHOD_INVALID: '请选择退款方式。',
    REFUND_REASON_REQUIRED: '请输入退款原因。',
    REFUND_EXCEEDS_PAID_AMOUNT: '退款金额超过可退金额，请修改金额后重试。',
    REIMBURSEMENT_PURPOSE_REQUIRED: '请输入报销用途。',
    REIMBURSEMENT_AMOUNT_INVALID: '请输入大于 0 的报销金额。',
    REIMBURSEMENT_STATUS_INVALID: '当前报销状态不能操作，请刷新后查看最新状态。',
    CUSTOMER_NOT_FOUND: '客户不存在，请重新选择或录入客户信息。',
    VEHICLE_NOT_FOUND: '车辆不存在，请重新选择车辆。',
    VEHICLE_NOT_IN_CUSTOMER: '所选车辆不属于当前客户，请重新选择。',
    FORBIDDEN: '当前账号没有此功能权限，请联系门店管理员开通。',
    UNAUTHORIZED: '登录状态已失效，请重新登录。',
    PASSWORD_CHANGE_REQUIRED: '当前账号需要先修改初始密码，请到管理端修改后再使用小程序。',
    WECHAT_NOT_BOUND: '该微信未绑定员工账号，请先用账号密码登录并绑定微信。',
    WECHAT_LOGIN_FAILED: '微信登录失败，请稍后重试或改用账号密码登录。',
    CAPTCHA_REQUIRED: '请输入验证码。',
    CAPTCHA_INVALID: '验证码错误，请重新输入。',
    CAPTCHA_EXPIRED: '验证码已过期，请刷新后重试。',
    CAPTCHA_LIMIT_EXCEEDED: '验证码请求过于频繁，请稍后重试。',
  };
  const key = code == null ? '' : String(code);
  return map[key] || message || '请求失败';
}
