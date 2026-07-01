
const PROGRESS_STATUS_MAP: Record<string, string> = {
  DRAFT: '新建中',
  REPAIRING: '维修中',
  REPAIR_DONE: '维修完成',
  DELIVERED: '已交付',
  CANCELLED: '已取消',
};

export const getProgressStatusText = (status?: string | null, text?: string | null) => {
  if (!status) return '-';
  if (PROGRESS_STATUS_MAP[status]) return PROGRESS_STATUS_MAP[status];
  return `未知状态（${status}）`;
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
  return map[status] || `未知状态（${status}）`;
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
  return map[status] || `未知状态（${status}）`;
};

export const getNoChargeReasonText = (reason?: string | null) => {
  if (!reason) return '未填写';
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
};

export const getFriendlyErrorMessage = (code?: string, message?: string) => {
  const map: Record<string, string> = {
    COMMON_BAD_REQUEST: '请求参数有误，请检查填写内容后重试。',
    COMMON_NOT_FOUND: '未找到对应数据，可能已被删除或不在当前门店。',
    COMMON_INTERNAL_ERROR: '系统暂时无法完成请求，请稍后重试或联系管理员。',
    WORK_ORDER_LEGACY_SETTLE_DISABLED: '状态模型已升级，该工单无法执行此操作。',
    WORK_ORDER_LEGACY_STATUS_EXISTS: '存在未兼容的业务数据，请联系系统管理员。',
    PART_NAME_REQUIRED: '请填写配件名称。',
    PART_CODE_DUPLICATE: '配件编码已存在，请更换后重试。',
    PART_OFFICIAL_CODE_REQUIRED: '官方配件必须填写官方品号。',
    PART_NOT_FOUND: '配件不存在或不属于当前门店。',
    PART_DISABLED: '该配件已停用，请先启用后再操作。',
    PART_HAS_STOCK: '该配件仍有库存或业务引用，不能直接删除。',
    PART_STOCK_NOT_FOUND: '未找到该配件的库存记录。',
    INVENTORY_QTY_MUST_POSITIVE: '入库数量必须大于 0。',
    INBOUND_UNIT_COST_NEGATIVE: '入库单价不能为负数。',
    INVENTORY_ADJUST_ZERO: '库存调整数量不能为 0。',
    INVENTORY_ADJUST_REASON_REQUIRED: '请选择库存调整原因。',
    INVENTORY_ADJUST_WOULD_NEGATIVE: '调整后可用库存不能为负数。',
    INVENTORY_ADJUST_ACTUAL_NEGATIVE: '调整后实际库存不能为负数。',
    INVENTORY_AVAILABLE_NOT_ENOUGH: '可用库存不足，无法完成操作。',
    WORK_ORDER_NOT_FOUND: '工单不存在或不属于当前门店。',
    WORK_ORDER_NOT_DRAFT: '工单已不是草稿状态，不能继续编辑。',
    PAYMENT_AMOUNT_INVALID: '收款金额必须大于 0。',
    PAYMENT_METHOD_INVALID: '请选择有效的收款方式。',
    PAYMENT_EXCEEDS_RECEIVABLE: '收款金额超过待收金额。',
    REFUND_AMOUNT_INVALID: '退款金额必须大于 0。',
    REFUND_METHOD_INVALID: '请选择有效的退款方式。',
    REFUND_REASON_REQUIRED: '请填写退款原因。',
    REFUND_EXCEEDS_PAID_AMOUNT: '退款金额超过可退金额。',
    WORK_ORDER_SETTLE_NOT_ALLOWED: '当前工单状态不允许结算。',
    WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH: '实收金额不足，无法结算。',
    WORK_ORDER_REPAIR_DONE_NOT_ALLOWED: '当前工单状态不允许标记维修完成。',
    WORK_ORDER_DELIVER_NOT_ALLOWED: '当前工单状态不允许交付关闭。',
    WORK_ORDER_NO_CHARGE_REASON_REQUIRED: '请选择无需收款原因。',
    WORK_ORDER_CANCEL_NOT_ALLOWED: '当前工单状态不允许取消。',
    WORK_ORDER_CANCEL_REASON_REQUIRED: '请填写取消原因。',
    OFFICIAL_AFTER_SALES_NOT_FOUND: '官方售后记录不存在。',
    OFFICIAL_ORDER_NO_REQUIRED: '请填写官方售后订单号。',
    OFFICIAL_ORDER_NO_DUPLICATED: '官方售后订单号已存在。',
    OFFICIAL_SETTLEMENT_AMOUNT_INVALID: '官方结算金额必须大于 0。',
    OFFICIAL_SETTLEMENT_STATUS_INVALID: '官方结算状态无效。',
    OFFICIAL_SETTLEMENT_NOT_ALLOWED: '当前工单不允许标记官方结算。',
    WORK_ORDER_NOT_OFFICIAL_AFTER_SALES: '该工单不是官方售后工单。',
    REIMBURSEMENT_NOT_FOUND: '报销记录不存在或不属于当前门店。',
    REIMBURSEMENT_AMOUNT_INVALID: '报销金额必须大于 0。',
    REIMBURSEMENT_CONFIRMED_AMOUNT_INVALID: '确认金额必须大于 0。',
    REIMBURSEMENT_PURPOSE_REQUIRED: '请填写报销用途。',
    REIMBURSEMENT_STATUS_INVALID: '当前报销状态不允许操作。',
    REIMBURSEMENT_REJECT_REASON_REQUIRED: '请填写驳回原因。',
    USER_NOT_FOUND: '用户不存在。',
    USERNAME_DUPLICATED: '用户名已存在。',
    PHONE_DUPLICATED: '手机号已存在。',
    ROLE_NOT_FOUND: '角色不存在。',
    PASSWORD_INVALID: '密码不符合复杂度要求。',
    PASSWORD_CHANGE_REQUIRED: '当前账号必须先修改密码。',
    OLD_PASSWORD_INCORRECT: '原密码错误。',
    USER_DISABLE_NOT_ALLOWED: '当前用户不允许停用。',
    USER_OPERATION_NOT_ALLOWED: '当前用户不允许执行该操作。',
    STORE_NOT_FOUND: '门店不存在。',
    STORE_NAME_REQUIRED: '请填写门店名称。',
    CUSTOMER_NOT_FOUND: '客户不存在。',
    CUSTOMER_PHONE_DUPLICATED: '该手机号已存在客户档案。',
    CUSTOMER_HAS_ACTIVE_ORDERS: '该客户仍有关联的未完成工单，请先处理完成后再删除。',
    VEHICLE_NOT_FOUND: '车辆不存在。',
    VEHICLE_FRAME_NO_DUPLICATED: '该车架号已存在。',
    VEHICLE_HAS_ACTIVE_ORDERS: '该车辆仍有关联的未完成工单，请先处理完成后再删除。',
    PLATFORM_STORE_CONTEXT_REQUIRED: '平台账号请先选择门店上下文。',
    PLATFORM_ACCESS_DENIED: '平台账号不能访问门店业务接口。',
    STORE_NAME_DUPLICATED: '门店名称已存在。',
    STORE_CODE_DUPLICATED: '门店编码已存在。',
    STORE_CODE_GENERATE_FAILED: '门店编码生成失败，请重试。',
    INVALID_STORE_STATUS: '门店状态值无效。',
    CAPTCHA_REQUIRED: '请输入验证码。',
    CAPTCHA_INVALID: '验证码错误，请重新输入。',
    CAPTCHA_EXPIRED: '验证码已过期，请刷新后重试。',
    CAPTCHA_LIMIT_EXCEEDED: '验证码请求过于频繁，请稍后重试。',
    FORBIDDEN: '当前账号没有执行此操作的权限，请联系门店管理员调整角色。',
    UNAUTHORIZED: '登录状态已失效，请重新登录。',
  };
  return (code && map[code]) || message || '请求失败';
};
