<template>
  <PageContainer title="工单管理" description="查看维修工单、收银状态与官方售后标记">
    <el-alert
      title="管理端支持工单查看与收银操作。创建工单、提交/取消等现场操作由员工小程序端承接。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="工单号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入工单号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户姓名" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="车架号">
          <el-input v-model="queryParams.vin" placeholder="请输入车架号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="进度状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="新建中" value="DRAFT" />
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="维修完成" value="REPAIR_DONE" />
            <el-option label="已交付" value="DELIVERED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="收银状态">
          <el-select v-model="queryParams.cashierStatus" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="无需收款" value="NO_CHARGE" />
            <el-option label="未收款" value="UNPAID" />
            <el-option label="部分收款" value="PARTIAL_PAID" />
            <el-option label="已收齐" value="PAID" />
            <el-option label="待退款" value="REFUND_PENDING" />
            <el-option label="部分退款" value="PARTIAL_REFUNDED" />
            <el-option label="已退清" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="官方售后">
          <el-select v-model="queryParams.isOfficial" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建日期">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px;"
          />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">

      <div class="table-wrapper">
<el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%; min-width: 1000px"
        border
      >
        <el-table-column prop="orderNo" label="工单编号" width="160" />
        <el-table-column prop="customerName" label="客户姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="scooterModel" label="车型" width="100" />
        <el-table-column prop="vin" label="车架号" width="160" show-overflow-tooltip />
        <el-table-column label="工单进度" width="110" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.progressStatus || row.status" :label="getProgressStatusText(row.progressStatus || row.status, row.progressStatusText)" />
          </template>
        </el-table-column>
        <el-table-column label="收银状态" width="110" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.cashierStatus || 'UNPAID'" :label="getCashierStatusText(row.cashierStatus, row.cashierStatusText)" />
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="110" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.inventoryStatus || 'NOT_RESERVED'" :label="getInventoryStatusText(row.inventoryStatus, row.inventoryStatusText)" />
          </template>
        </el-table-column>
        <el-table-column label="应收金额" width="100" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.receivableAmount" />
          </template>
        </el-table-column>
        <el-table-column label="净实收" width="100" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.netReceived ?? row.actualAmount" type="success" />
          </template>
        </el-table-column>
        <el-table-column label="待收金额" width="100" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.outstandingAmount ?? Math.max(0, row.receivableAmount - row.actualAmount)" :type="(row.outstandingAmount ?? 0) > 0 ? 'warning' : 'info'" />
          </template>
        </el-table-column>
        <el-table-column label="是否官方售后" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isOfficial ? 'success' : 'info'" size="small">
              {{ row.isOfficial ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="官方订单号" width="160">
          <template #default="{ row }">
            {{ row.officialOrderNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
</div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="工单详情" size="800px">
      <template v-if="currentOrder">
        <el-divider content-position="left">基础信息</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="工单进度"><StatusTag :status="currentOrder.progressStatus || currentOrder.status" :label="getProgressStatusText(currentOrder.progressStatus || currentOrder.status, currentOrder.progressStatusText)" /></el-descriptions-item>
          <el-descriptions-item label="收银状态"><StatusTag :status="currentOrder.cashierStatus || 'UNPAID'" :label="getCashierStatusText(currentOrder.cashierStatus, currentOrder.cashierStatusText)" /></el-descriptions-item>
          <el-descriptions-item label="库存状态"><StatusTag :status="currentOrder.inventoryStatus || 'NOT_RESERVED'" :label="getInventoryStatusText(currentOrder.inventoryStatus, currentOrder.inventoryStatusText)" /></el-descriptions-item>
          <el-descriptions-item label="客户姓名">{{ currentOrder.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentOrder.phone }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ currentOrder.scooterModel }}</el-descriptions-item>
          <el-descriptions-item label="车架号">{{ currentOrder.vin }}</el-descriptions-item>
          <el-descriptions-item label="电池号">{{ currentOrder.batteryNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="无需收款原因" v-if="currentOrder.noChargeReason" :span="2">
            <el-tag type="info">{{ getNoChargeReasonText(currentOrder.noChargeReason) }}</el-tag>
            <span v-if="currentOrder.noChargeRemark" style="margin-left: 8px; color: #606266;">{{ currentOrder.noChargeRemark }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="维修项目" :span="2">{{ currentOrder.repairItem || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">费用明细</el-divider>
        <el-table :data="currentOrder.chargeItems" border size="small">
          <el-table-column label="费用类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getFeeTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="项目名称" min-width="150" />
          <el-table-column label="配件编码" width="120">
            <template #default="{ row }">
              {{ row.partCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.unitPrice" /></template>
          </el-table-column>
          <el-table-column label="行金额" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.lineAmount" /></template>
          </el-table-column>
          <el-table-column label="成本金额" width="100" align="right">
            <template #default="{ row }"><MoneyText :amount="row.costAmount || '-' " /></template>
          </el-table-column>
          <el-table-column label="影响库存" width="90" align="center">
            <template #default="{ row }">
              {{ row.affectsInventory ? '是' : '否' }}
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">收银与售后摘要</el-divider>
        <el-descriptions :column="4" class="payment-summary" direction="vertical" border size="small">
          <el-descriptions-item label="应收金额" align="center"><MoneyText :amount="currentOrder.receivableAmount" bold /></el-descriptions-item>
          <el-descriptions-item label="收款总额" align="center"><MoneyText :amount="currentOrder.paidAmount" /></el-descriptions-item>
          <el-descriptions-item label="退款总额" align="center"><MoneyText :amount="currentOrder.refundedAmount" type="danger" /></el-descriptions-item>
          <el-descriptions-item label="实收净额" align="center"><MoneyText :amount="currentOrder.netReceived ?? currentOrder.actualAmount" bold type="success" /></el-descriptions-item>
        </el-descriptions>

        <el-descriptions :column="2" border size="small" style="margin-bottom: 20px;">
          <el-descriptions-item label="是否官方售后">{{ currentOrder.isOfficial ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="官方订单号">{{ currentOrder.officialOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官方结算状态">{{ getOfficialSettlementLabel(currentOrder.officialSettlementStatus) }}</el-descriptions-item>
          <el-descriptions-item label="官方结算金额"><MoneyText :amount="currentOrder.officialSettlementAmount || 0" /></el-descriptions-item>
        </el-descriptions>

        <!-- 工单控制台与收银区域 -->
        <el-divider content-position="left">工单控制台 & 财务收银</el-divider>

        <el-alert
          v-if="nextStepTip"
          :title="'指引：' + nextStepTip"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        />

        <el-descriptions :column="4" direction="vertical" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="应收金额" align="center">
            <MoneyText :amount="currentOrder.receivableAmount" bold />
          </el-descriptions-item>
          <el-descriptions-item label="实收净额" align="center">
            <MoneyText :amount="currentOrder.netReceived ?? currentOrder.actualAmount" bold type="success" />
          </el-descriptions-item>
          <el-descriptions-item label="待收金额" align="center">
            <MoneyText :amount="currentOrder.outstandingAmount ?? pendingAmount" bold :type="(currentOrder.outstandingAmount ?? pendingAmount) > 0 ? 'warning' : 'info'" />
          </el-descriptions-item>
          <el-descriptions-item label="可退金额" align="center">
            <MoneyText :amount="currentOrder.refundableAmount ?? 0" :type="(currentOrder.refundableAmount ?? 0) > 0 ? 'danger' : 'info'" />
          </el-descriptions-item>
        </el-descriptions>

        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px;">
          <el-button
            v-if="currentOrder.canMarkRepairDone"
            type="success"
            size="small"
            @click="openMarkRepairDoneDialog"
          >标记维修完成</el-button>

          <!-- 交付关闭按钮：当已完工但未结清/不可交付时，显示置灰按钮与“请先收齐尾款后再交付关闭”的友好提示 -->
          <template v-if="currentOrder.progressStatus === 'REPAIR_DONE'">
            <el-tooltip
              v-if="!currentOrder.canDeliver || !['PAID', 'NO_CHARGE'].includes(currentOrder.cashierStatus || '')"
              content="请先收齐尾款后再交付关闭"
              placement="top"
            >
              <span style="display: inline-block;">
                <el-button
                  type="primary"
                  size="small"
                  disabled
                >交付关闭工单</el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              type="primary"
              size="small"
              @click="openDeliverDialog"
            >交付关闭工单</el-button>
          </template>

          <el-button
            v-if="currentOrder.canRecordPayment"
            type="primary"
            size="small"
            plain
            @click="openPaymentDialog"
          >记录收款</el-button>

          <el-button
            v-if="currentOrder.canRecordRefund"
            type="warning"
            size="small"
            plain
            @click="openRefundDialog(false)"
          >记录退款</el-button>

          <el-button
            v-if="currentOrder.canRefundAfterDelivery"
            type="danger"
            size="small"
            plain
            @click="openRefundDialog(true)"
          >交付后退款</el-button>

          <el-button
            v-if="['DRAFT', 'REPAIRING', 'REPAIR_DONE'].includes(currentOrder.progressStatus || '')"
            type="warning"
            size="small"
            @click="openAddChargeDialog"
          >追加非库存费用</el-button>

          <el-button
            v-if="currentOrder.canCancel"
            type="danger"
            size="small"
            @click="openCancelDialog"
          >取消工单</el-button>
        </div>

        <div v-if="workOrderPayments.length > 0" style="margin-bottom: 12px;">
          <div style="font-weight: 600; margin-bottom: 6px; font-size: 14px;">收款记录</div>
          <el-table :data="workOrderPayments" border size="small">
            <el-table-column label="收款单号" prop="paymentNo" width="180" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }"><MoneyText :amount="row.amount" /></template>
            </el-table-column>
            <el-table-column label="收款方式" width="100">
              <template #default="{ row }">{{ getPaymentMethodLabel(row.paymentMethod) }}</template>
            </el-table-column>
            <el-table-column label="收款时间" prop="paidAt" width="160" />
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>

        <div v-if="workOrderRefunds.length > 0" style="margin-bottom: 12px;">
          <div style="font-weight: 600; margin-bottom: 6px; font-size: 14px;">退款记录</div>
          <el-table :data="workOrderRefunds" border size="small">
            <el-table-column label="退款单号" prop="refundNo" width="180" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }"><MoneyText :amount="row.amount" type="danger" /></template>
            </el-table-column>
            <el-table-column label="退款方式" width="100">
              <template #default="{ row }">{{ getPaymentMethodLabel(row.refundMethod) }}</template>
            </el-table-column>
            <el-table-column label="退款原因" prop="reason" min-width="120" show-overflow-tooltip />
            <el-table-column label="退款时间" prop="refundedAt" width="160" />
          </el-table>
        </div>
      </template>
    </el-drawer>

    <!-- 记录收款弹窗 -->
    <el-dialog v-model="paymentDialogVisible" title="记录收款" width="420px" :close-on-click-modal="false">
      <el-form :model="paymentForm" label-width="80px" size="default">
        <el-form-item label="收款金额">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0.01"
            :max="currentOrder?.outstandingAmount ?? 0"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            待收金额：<MoneyText :amount="currentOrder?.outstandingAmount ?? 0" />
          </div>
        </el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="paymentForm.paymentMethod" placeholder="请选择" style="width: 100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPayment">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 记录退款弹窗 -->
    <el-dialog v-model="refundDialogVisible" :title="isRefundAfterDelivery ? '交付后退款' : '记录退款'" width="440px" :close-on-click-modal="false">
      <el-form :model="refundForm" label-width="80px" size="default">
        <el-alert
          v-if="isRefundAfterDelivery"
          title="重要提示：交付后退款只影响资金流水，不回滚库存，不重开工单。"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        />
        <el-form-item label="退款金额">
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :max="currentOrder?.refundableAmount ?? 0"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            最大可退金额：<MoneyText :amount="currentOrder?.refundableAmount ?? 0" />
          </div>
        </el-form-item>
        <el-form-item label="退款方式">
          <el-select v-model="refundForm.refundMethod" placeholder="请选择" style="width: 100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input v-model="refundForm.reason" placeholder="必填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="refundForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>

    <!-- 标记维修完成弹窗 -->
    <el-dialog v-model="repairDoneDialogVisible" title="标记维修完成" width="460px" :close-on-click-modal="false">
      <div style="margin-bottom: 16px; font-size: 14px;">
        确认标记此工单为维修完成状态？此操作会推进工单进度。
      </div>
      <el-form v-if="currentOrder && currentOrder.receivableAmount === 0" :model="repairDoneForm" label-width="110px" size="default">
        <el-alert
          title="当前应收金额为 ￥0，必须填报无需收款原因。"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        />
        <el-form-item label="无需收款原因" required>
          <el-select v-model="repairDoneForm.noChargeReason" placeholder="请选择原因" style="width: 100%">
            <el-option label="保修内免费" value="WARRANTY_FREE" />
            <el-option label="首保赠送" value="FIRST_MAINTENANCE_FREE" />
            <el-option label="客户自备配件" value="CUSTOMER_OWN_PARTS" />
            <el-option label="无收费项目" value="NO_CHARGE_ITEM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="repairDoneForm.noChargeReason === 'OTHER'" label="其他原因说明" required>
          <el-input v-model="repairDoneForm.otherReason" placeholder="请输入具体原因" />
        </el-form-item>
        <el-form-item label="无需收款备注">
          <el-input v-model="repairDoneForm.noChargeRemark" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <el-form v-else :model="repairDoneForm" label-width="80px" size="default">
        <el-form-item label="处理备注">
          <el-input v-model="repairDoneForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDoneDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitRepairDone">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- 交付关闭确认弹窗 -->
    <el-dialog v-model="deliverDialogVisible" title="交付关闭工单" width="460px" :close-on-click-modal="false">
      <div v-if="currentOrder" style="margin-bottom: 16px;">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 12px;">
          <el-descriptions-item label="应收金额"><MoneyText :amount="currentOrder.receivableAmount" bold /></el-descriptions-item>
          <el-descriptions-item label="实收净额"><MoneyText :amount="currentOrder.netReceived ?? currentOrder.actualAmount" bold type="success" /></el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.cashierStatus === 'NO_CHARGE'" label="无需收款原因">
            <span style="font-weight: 600; color: #e6a23c;">{{ getNoChargeReasonText(currentOrder.noChargeReason) || '未填写' }}</span>
          </el-descriptions-item>
        </el-descriptions>
        
        <el-alert
          v-if="currentOrder.cashierStatus === 'NO_CHARGE'"
          :title="`无需收款工单（原因：${getNoChargeReasonText(currentOrder.noChargeReason) || '未填写'}）。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。`"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 12px;"
        />
        <el-alert
          v-else
          title="工单已结清。确认交付关闭后，工单将进入已交付状态；库存已在标记维修完成时扣减，本操作不再改变库存。"
          type="success"
          show-icon
          :closable="false"
          style="margin-bottom: 12px;"
        />
      </div>
      <el-form label-width="80px" size="default">
        <el-form-item label="交付备注">
          <el-input v-model="deliverRemark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submitDeliver"
        >确认交付并关闭</el-button>
      </template>
    </el-dialog>

    <!-- 追加非库存费用弹窗 -->
    <el-dialog v-model="addChargeDialogVisible" title="追加非库存费用" width="460px" :close-on-click-modal="false">
      <el-form :model="chargeForm" label-width="90px" size="default">
        <el-alert
          title="仅支持追加工时费或其它杂费，不影响配件库存。"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        />
        <el-form-item label="费用类型" required>
          <el-select v-model="chargeForm.chargeType" placeholder="请选择" style="width: 100%">
            <el-option label="工时费" value="LABOR" />
            <el-option label="其他费用" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" required>
          <el-input v-model="chargeForm.itemName" placeholder="如：电路检修、超时保管费" />
        </el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="chargeForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="chargeForm.quantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="追加原因" required>
          <el-input v-model="chargeForm.reason" placeholder="必填，说明追加费用的原委" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="chargeForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addChargeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAddCharge">确认追加</el-button>
      </template>
    </el-dialog>

    <!-- 取消工单弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消工单" width="460px" :close-on-click-modal="false">
      <el-form :model="cancelForm" label-width="80px" size="default">
        <el-alert
          title="警告：取消工单是不可逆操作！工单取消后将自动释放预占库存。若有已收款项，需要手动在工单详情中记录退款以保持财务账目平衡。"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        />
        <el-form-item label="取消原因" required>
          <el-input v-model="cancelForm.reason" type="textarea" :rows="3" placeholder="请输入取消原因（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitCancel">确认取消工单</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';
import MoneyText from '@/components/MoneyText.vue';
import {
  getWorkOrderList,
  getWorkOrderDetail,
  getWorkOrderPayments,
  getWorkOrderRefunds,
  recordPayment,
  recordRefund,
  markRepairDone,
  deliverWorkOrder,
  addNonInventoryCharge,
  cancelWorkOrder,
} from '@/api/workOrder';
import { useAuthStore } from '@/stores/auth';
import {
  getCashierStatusText,
  getInventoryStatusText,
  getNoChargeReasonText,
  getProgressStatusText,
} from '@/utils/statusText';
import type { WorkOrderRecord, WorkOrderQuery } from '@/types/workOrder';

const authStore = useAuthStore();

// 查询参数
const queryParams = reactive<WorkOrderQuery>({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  customerName: '',
  phone: '',
  vin: '',
  status: '',
  progressStatus: '',
  cashierStatus: '',
  isOfficial: '',
  dateRange: undefined,
});

const loading = ref(false);
const tableData = ref<WorkOrderRecord[]>([]);
const total = ref(0);

const getFeeTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    PART: '配件费',
    LABOR: '工时费',
    OTHER: '其他费用',
  };
  return map[type] || '其他费用';
};

const getOfficialSettlementLabel = (status?: string) => {
  if (!status) return '-';
  const map: Record<string, string> = {
    NOT_REQUIRED: '无需结算',
    PENDING: '待结算',
    SETTLED: '已结算',
  };
  return map[status] || '-';
};

const getPaymentMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    WECHAT: '微信',
    ALIPAY: '支付宝',
    UNIONPAY: '银联',
    CASH: '现金',
    OTHER: '其他',
  };
  return map[method] || '其他';
};

// 获取列表数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getWorkOrderList(queryParams);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    // request interceptor already shows error
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.orderNo = '';
  queryParams.customerName = '';
  queryParams.phone = '';
  queryParams.vin = '';
  queryParams.status = '';
  queryParams.progressStatus = '';
  queryParams.cashierStatus = '';
  queryParams.isOfficial = '';
  queryParams.dateRange = undefined;
  handleSearch();
};

// 详情抽屉
const drawerVisible = ref(false);
const currentOrder = ref<WorkOrderRecord | null>(null);
const workOrderPayments = ref<any[]>([]);
const workOrderRefunds = ref<any[]>([]);

const pendingAmount = computed(() => {
  if (!currentOrder.value) return 0;
  return Math.max(0, currentOrder.value.receivableAmount - currentOrder.value.actualAmount);
});

// 下一步动作指引
const nextStepTip = computed(() => {
  if (!currentOrder.value) return '';
  const progress = currentOrder.value.progressStatus || currentOrder.value.status;
  const cashier = currentOrder.value.cashierStatus;
  const inventory = currentOrder.value.inventoryStatus;

  if (progress === 'DRAFT') {
    return '待员工小程序端提交并预占库存以推进工单';
  }
  if (progress === 'REPAIRING') {
    if (inventory === 'NOT_RESERVED') {
      return '维修中，待员工端发起库存预占以正式锁定配件';
    }
    if (cashier === 'UNPAID') {
      return '维修进行中，客户尚未付款。支持线下提前收款及财务登记';
    }
    if (cashier === 'PARTIAL_PAID') {
      return '维修进行中，客户已部分付款。支持继续登记收款';
    }
    if (cashier === 'PAID') {
      return '维修进行中，客户已全额结清。等待维修完成后标记完成';
    }
    return '维修进行中。待完成后标记维修完成';
  }
  if (progress === 'REPAIR_DONE') {
    if (cashier === 'UNPAID') {
      return '工单维修已完成。客户尚未付款，请先催款/记录收款，结清后即可交付关闭';
    }
    if (cashier === 'PARTIAL_PAID') {
      return '工单维修已完成。客户已部分付款，待结清后即可交付关闭';
    }
    if (cashier === 'PAID' || cashier === 'NO_CHARGE') {
      return '工单维修已完成且款项已结清，请执行“交付关闭工单”操作。库存已在标记维修完成时扣减，交付关闭不再改变库存';
    }
    return '工单维修已完成。支持执行交付关闭';
  }
  if (progress === 'DELIVERED') {
    if (cashier === 'REFUND_PENDING' || cashier === 'PARTIAL_REFUNDED') {
      return '工单已交付结清，目前存在待售后退款款项，请尽快处理退款';
    }
    return '工单已顺利交付并关闭，业务流程已完结';
  }
  if (progress === 'CANCELLED') {
    return '工单已取消，库存已自动释放';
  }
  return '此工单属于旧试运行状态数据，建议清理数据以适配新业务流程';
});

const submitting = ref(false);

const handleView = async (row: WorkOrderRecord) => {
  drawerVisible.value = true;
  currentOrder.value = null;
  workOrderPayments.value = [];
  workOrderRefunds.value = [];
  try {
    currentOrder.value = await getWorkOrderDetail(row.id);
    await loadCashierRecords(row.id);
  } catch {
    // request interceptor already shows error
  }
};

const loadCashierRecords = async (workOrderId: string) => {
  const [paymentsResult, refundsResult] = await Promise.allSettled([
    getWorkOrderPayments(workOrderId),
    getWorkOrderRefunds(workOrderId),
  ]);
  if (paymentsResult.status === 'fulfilled') {
    workOrderPayments.value = paymentsResult.value || [];
  } else {
    ElMessage.error('收款记录加载失败');
    workOrderPayments.value = [];
  }
  if (refundsResult.status === 'fulfilled') {
    workOrderRefunds.value = refundsResult.value || [];
  } else {
    ElMessage.error('退款记录加载失败');
    workOrderRefunds.value = [];
  }
};

const refreshDetail = async () => {
  if (!currentOrder.value) return;
  const id = currentOrder.value.id;
  try {
    currentOrder.value = await getWorkOrderDetail(id);
    await loadCashierRecords(id);
  } catch {
    // ignore
  }
};

// ── 收款弹窗 ──
const paymentDialogVisible = ref(false);
const paymentForm = reactive({ amount: 0.01, paymentMethod: '', remark: '' });

const openPaymentDialog = () => {
  if (!currentOrder.value) return;
  paymentForm.amount = Math.max(0.01, currentOrder.value.outstandingAmount ?? 0);
  paymentForm.paymentMethod = '';
  paymentForm.remark = '';
  paymentDialogVisible.value = true;
};

const submitPayment = async () => {
  if (!currentOrder.value) return;
  if (!paymentForm.paymentMethod) {
    ElMessage.warning('请选择收款方式');
    return;
  }
  if (paymentForm.amount <= 0) {
    ElMessage.warning('收款金额必须大于0');
    return;
  }
  const maxAmount = currentOrder.value.outstandingAmount ?? 0;
  if (paymentForm.amount > maxAmount) {
    ElMessage.warning('收款金额不能超过待收金额');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认收款 <b>￥${paymentForm.amount.toFixed(2)}</b>，收款方式：${getPaymentMethodLabel(paymentForm.paymentMethod)}？`,
      '确认收款',
      { confirmButtonText: '确认', cancelButtonText: '取消', dangerouslyUseHTMLString: true },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await recordPayment(currentOrder.value.id, {
      amount: paymentForm.amount,
      paymentMethod: paymentForm.paymentMethod,
      receiverId: authStore.user!.userId,
      remark: paymentForm.remark || undefined,
    });
    ElMessage.success('收款成功');
    paymentDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};

// ── 退款弹窗 ──
const refundDialogVisible = ref(false);
const isRefundAfterDelivery = ref(false);
const refundForm = reactive({ amount: 0.01, refundMethod: '', reason: '', remark: '' });

const openRefundDialog = (afterDelivery: boolean = false) => {
  if (!currentOrder.value) return;
  isRefundAfterDelivery.value = afterDelivery;
  refundForm.amount = Math.max(0.01, currentOrder.value.refundableAmount ?? 0);
  refundForm.refundMethod = '';
  refundForm.reason = '';
  refundForm.remark = '';
  refundDialogVisible.value = true;
};

const submitRefund = async () => {
  if (!currentOrder.value) return;
  if (!refundForm.refundMethod) {
    ElMessage.warning('请选择退款方式');
    return;
  }
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因');
    return;
  }
  if (refundForm.amount <= 0) {
    ElMessage.warning('退款金额必须大于0');
    return;
  }
  const maxRefund = currentOrder.value.refundableAmount ?? 0;
  if (refundForm.amount > maxRefund) {
    ElMessage.warning(`退款金额不能超过最大可退金额：￥${maxRefund.toFixed(2)}`);
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认退款 <b>￥${refundForm.amount.toFixed(2)}</b>，退款方式：${getPaymentMethodLabel(refundForm.refundMethod)}？`,
      '确认退款',
      { confirmButtonText: '确认', cancelButtonText: '取消', dangerouslyUseHTMLString: true, type: 'warning' },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await recordRefund(currentOrder.value.id, {
      amount: refundForm.amount,
      refundMethod: refundForm.refundMethod,
      reason: refundForm.reason,
      remark: refundForm.remark || undefined,
    });
    ElMessage.success('退款成功');
    refundDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};

// ── 标记维修完成弹窗 ──
const repairDoneDialogVisible = ref(false);
const repairDoneForm = reactive({
  noChargeReason: '',
  otherReason: '',
  noChargeRemark: '',
  remark: ''
});

const openMarkRepairDoneDialog = () => {
  repairDoneForm.noChargeReason = '';
  repairDoneForm.otherReason = '';
  repairDoneForm.noChargeRemark = '';
  repairDoneForm.remark = '';
  repairDoneDialogVisible.value = true;
};

const submitRepairDone = async () => {
  if (!currentOrder.value) return;
  const isNoCharge = currentOrder.value.receivableAmount === 0;
  let finalNoChargeReason = '';
  let finalNoChargeRemark = '';
  let finalRemark = '';

  if (isNoCharge) {
    if (!repairDoneForm.noChargeReason) {
      ElMessage.warning('请选择无需收款原因');
      return;
    }
    if (repairDoneForm.noChargeReason === 'OTHER') {
      if (!repairDoneForm.otherReason.trim()) {
        ElMessage.warning('请输入其他原因具体说明');
        return;
      }
      finalNoChargeReason = repairDoneForm.otherReason.trim();
    } else {
      finalNoChargeReason = repairDoneForm.noChargeReason;
    }
    finalNoChargeRemark = repairDoneForm.noChargeRemark.trim();
  } else {
    finalRemark = repairDoneForm.remark.trim();
  }

  try {
    await ElMessageBox.confirm('确认将工单标记为维修完成？', '确认操作');
  } catch {
    return;
  }

  submitting.value = true;
  try {
    await markRepairDone(currentOrder.value.id, {
      noChargeReason: isNoCharge ? finalNoChargeReason : undefined,
      noChargeRemark: isNoCharge ? finalNoChargeRemark : undefined,
      remark: !isNoCharge ? finalRemark : undefined,
    });
    ElMessage.success('工单已成功标记为维修完成');
    repairDoneDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};
// ── 交付关闭工单弹窗 ──
const deliverDialogVisible = ref(false);
const deliverRemark = ref('');

const openDeliverDialog = () => {
  if (!currentOrder.value || !currentOrder.value.canDeliver || !['PAID', 'NO_CHARGE'].includes(currentOrder.value.cashierStatus || '') || currentOrder.value.progressStatus !== 'REPAIR_DONE') {
    ElMessage.error('请先收齐尾款后再交付关闭');
    return;
  }
  deliverRemark.value = '';
  deliverDialogVisible.value = true;
};

const submitDeliver = async () => {
  if (!currentOrder.value) return;
  if (!currentOrder.value.canDeliver || !['PAID', 'NO_CHARGE'].includes(currentOrder.value.cashierStatus || '') || currentOrder.value.progressStatus !== 'REPAIR_DONE') {
    ElMessage.error('请先收齐尾款后再交付关闭');
    return;
  }
  try {
    await ElMessageBox.confirm('确认交付此工单并关闭？库存已在标记维修完成时扣减，交付关闭不再改变库存。', '确认交付', {
      confirmButtonText: '确认交付',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await deliverWorkOrder(currentOrder.value.id, {
      remark: deliverRemark.value ? deliverRemark.value.trim() : undefined,
    });
    ElMessage.success('工单交付成功并关闭');
    deliverDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};

// ── 追加非库存费用弹窗 ──
const addChargeDialogVisible = ref(false);
const chargeForm = reactive({
  chargeType: 'LABOR' as 'LABOR' | 'OTHER',
  itemName: '',
  unitPrice: 0.00,
  quantity: 1,
  reason: '',
  remark: ''
});

const openAddChargeDialog = () => {
  chargeForm.chargeType = 'LABOR';
  chargeForm.itemName = '';
  chargeForm.unitPrice = 0.00;
  chargeForm.quantity = 1;
  chargeForm.reason = '';
  chargeForm.remark = '';
  addChargeDialogVisible.value = true;
};

const submitAddCharge = async () => {
  if (!currentOrder.value) return;
  if (!chargeForm.itemName.trim()) {
    ElMessage.warning('请输入项目名称');
    return;
  }
  if (chargeForm.unitPrice < 0) {
    ElMessage.warning('单价不能小于0');
    return;
  }
  if (chargeForm.quantity <= 0) {
    ElMessage.warning('数量必须大于0');
    return;
  }
  if (!chargeForm.reason.trim()) {
    ElMessage.warning('请输入追加费用原因');
    return;
  }

  try {
    await ElMessageBox.confirm('确认追加该项非库存费用？', '确认追加');
  } catch {
    return;
  }

  submitting.value = true;
  try {
    await addNonInventoryCharge(currentOrder.value.id, {
      chargeType: chargeForm.chargeType,
      itemName: chargeForm.itemName.trim(),
      unitPrice: chargeForm.unitPrice,
      quantity: chargeForm.quantity,
      reason: chargeForm.reason.trim(),
      remark: chargeForm.remark ? chargeForm.remark.trim() : undefined,
    });
    ElMessage.success('非库存费用追加成功');
    addChargeDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};

// ── 取消工单弹窗 ──
const cancelDialogVisible = ref(false);
const cancelForm = reactive({
  reason: ''
});

const openCancelDialog = () => {
  cancelForm.reason = '';
  cancelDialogVisible.value = true;
};

const submitCancel = async () => {
  if (!currentOrder.value) return;
  if (!cancelForm.reason.trim()) {
    ElMessage.warning('请填写取消工单原因');
    return;
  }

  try {
    await ElMessageBox.confirm('工单取消后将自动释放预占库存，是否确认取消工单？', '警示确认', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '放弃'
    });
  } catch {
    return;
  }

  submitting.value = true;
  try {
    await cancelWorkOrder(currentOrder.value.id, {
      reason: cancelForm.reason.trim(),
    });
    ElMessage.success('工单取消成功');
    cancelDialogVisible.value = false;
    await refreshDetail();
    fetchData();
  } catch {
    // handled by request interceptor
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
.search-actions {
  margin-left: auto;
}
.table-card {
  min-height: 500px;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.payment-summary {
  margin-bottom: 20px;
}
:deep(.el-drawer__body) {
  padding-top: 0;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
