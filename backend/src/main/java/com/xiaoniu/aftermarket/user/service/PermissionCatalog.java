package com.xiaoniu.aftermarket.user.service;

import java.util.Map;
import java.util.Set;

public final class PermissionCatalog {

    private static final Set<String> PLATFORM_ONLY = Set.of(
            "PLATFORM_MANAGE"
    );

    private static final Map<String, PermissionMeta> META = Map.ofEntries(
            entry("USER_MANAGE", "SYSTEM", "系统管理", "menu.user", "员工管理", "PAGE", true),
            entry("ROLE_MANAGE", "SYSTEM", "系统管理", "menu.user", "角色权限", "PAGE", true),
            entry("DICT_MANAGE", "SYSTEM", "系统管理", "menu.dictionary", "基础配置", "PAGE", true),
            entry("STORE_MANAGE", "SYSTEM", "系统管理", "menu.store", "门店配置", "PAGE", true),
            entry("EXCEL_EXPORT", "SYSTEM", "系统管理", "menu.export", "数据导出", "BUTTON", true),

            entry("PLATFORM_MANAGE", "PLATFORM", "平台管理", "menu.platformStores", "门店平台管理", "PAGE", false),

            entry("PART_VIEW", "PART_INVENTORY", "配件库存", "menu.parts", "配件查看", "PAGE", true),
            entry("PART_CREATE", "PART_INVENTORY", "配件库存", "menu.parts", "新增配件", "BUTTON", true),
            entry("PART_MANAGE", "PART_INVENTORY", "配件库存", "menu.parts", "配件维护", "BUTTON", true),
            entry("INVENTORY_VIEW", "PART_INVENTORY", "配件库存", "menu.inventory", "库存查看", "PAGE", true),
            entry("INVENTORY_INBOUND", "PART_INVENTORY", "配件库存", "menu.inventory", "配件入库", "BUTTON", true),
            entry("INVENTORY_ADJUST", "PART_INVENTORY", "配件库存", "menu.inventory", "库存调整", "BUTTON", true),

            entry("WORK_ORDER_VIEW", "BUSINESS", "业务管理", "menu.workOrder", "工单查看", "PAGE", true),
            entry("WORK_ORDER_CREATE", "BUSINESS", "业务管理", "menu.workOrder", "工单创建", "BUTTON", true),
            entry("WORK_ORDER_UPDATE", "BUSINESS", "业务管理", "menu.workOrder", "工单编辑", "BUTTON", true),
            entry("WORK_ORDER_SUBMIT", "BUSINESS", "业务管理", "menu.workOrder", "工单提交", "BUTTON", true),
            entry("WORK_ORDER_CANCEL", "BUSINESS", "业务管理", "menu.workOrder", "工单取消", "BUTTON", true),
            entry("WORK_ORDER_SETTLE", "BUSINESS", "业务管理", "menu.workOrder", "工单结算", "BUTTON", true),
            entry("CUSTOMER_VIEW", "BUSINESS", "业务管理", "menu.customers", "客户车辆查看", "PAGE", true),
            entry("CUSTOMER_MANAGE", "BUSINESS", "业务管理", "menu.customers", "客户车辆维护", "BUTTON", true),

            entry("PAYMENT_RECORD", "FINANCE", "收银财务", "menu.payment", "收款记录", "BUTTON", true),
            entry("REFUND_RECORD", "FINANCE", "收银财务", "menu.refund", "退款记录", "BUTTON", true),
            entry("REFUND_AFTER_DELIVERY", "FINANCE", "收银财务", "menu.refund", "交付后退款", "BUTTON", true),
            entry("FINANCE_VIEW", "FINANCE", "收银财务", "menu.finance", "财务报表", "PAGE", true),
            entry("OFFICIAL_SETTLEMENT_MANAGE", "FINANCE", "收银财务", "menu.settlement", "官方结算", "BUTTON", true),
            entry("REIMBURSEMENT_SUBMIT", "FINANCE", "收银财务", "menu.reimbursement", "报销提交", "BUTTON", true),
            entry("REIMBURSEMENT_CONFIRM", "FINANCE", "收银财务", "menu.reimbursement", "报销确认", "BUTTON", true),

            entry("FUNDING_APPLICATION_VIEW", "FUNDING", "资方业务", "menu.funding", "资方资料查看", "PAGE", true),
            entry("FUNDING_APPLICATION_MANAGE", "FUNDING", "资方业务", "menu.funding", "资方资料维护", "BUTTON", true),
            entry("FUNDING_APPLICATION_AUDIT", "FUNDING", "资方业务", "menu.funding", "资方资料审核", "BUTTON", true),
            entry("FUNDING_CONTRACT_MANAGE", "FUNDING", "资方业务", "menu.funding", "资方合同管理", "BUTTON", true),
            entry("FUNDING_LEDGER_VIEW", "FUNDING", "资方业务", "menu.funding", "资方台账查看", "PAGE", true),
            entry("FUNDING_LEDGER_MANAGE", "FUNDING", "资方业务", "menu.funding", "资方台账维护", "BUTTON", true),
            entry("FUNDING_PAYMENT_RECORD", "FUNDING", "资方业务", "menu.funding", "资方收款登记", "BUTTON", true),
            entry("FUNDING_EXPORT", "FUNDING", "资方业务", "menu.funding", "资方台账导出", "BUTTON", true),
            entry("FUNDING_IMPORT", "FUNDING", "资方业务", "menu.funding", "资方台账导入", "BUTTON", true)
    );

    private PermissionCatalog() {
    }

    public static PermissionMeta meta(String permissionCode, String moduleCode, String permissionName) {
        return META.getOrDefault(permissionCode,
                new PermissionMeta(moduleCode, moduleCode, moduleCode, permissionName, "BUTTON",
                        isUnknownStoreGrantable(permissionCode)));
    }

    public static boolean isStoreGrantable(String permissionCode) {
        PermissionMeta meta = META.get(permissionCode);
        return meta == null ? isUnknownStoreGrantable(permissionCode) : meta.storeGrantable();
    }

    private static boolean isUnknownStoreGrantable(String permissionCode) {
        return !PLATFORM_ONLY.contains(permissionCode) && !permissionCode.contains(":");
    }

    private static Map.Entry<String, PermissionMeta> entry(String code, String moduleCode, String moduleName,
                                                           String resourceKey, String displayName,
                                                           String resourceType, boolean storeGrantable) {
        return Map.entry(code, new PermissionMeta(moduleCode, moduleName, resourceKey, displayName, resourceType, storeGrantable));
    }

    public record PermissionMeta(
            String moduleCode,
            String moduleName,
            String resourceKey,
            String displayName,
            String resourceType,
            boolean storeGrantable
    ) {
    }
}
