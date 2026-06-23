<template>
  <PageContainer title="资方台账" description="内部资料审核、线下合同留存、应收台账和收款维护">
    <div v-if="canViewSummary" class="summary-row">
      <el-card v-for="item in summaryCards" :key="item.label" shadow="never" class="summary-card">
        <div class="summary-label">{{ item.label }}</div>
        <div class="summary-value">{{ item.value }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane v-if="canUseApplicationTab" label="资料申请" name="applications">
          <div class="toolbar">
            <el-input v-if="canViewApplications" v-model="applicationQuery.keyword" placeholder="搜索姓名、电话、车型、组长" clearable style="width: 260px" @keyup.enter="loadApplications" />
            <el-select v-if="canViewApplications" v-model="applicationQuery.status" placeholder="状态" clearable style="width: 180px">
              <el-option v-for="item in applicationStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button v-if="canViewApplications" type="primary" @click="loadApplications">查询</el-button>
            <el-button v-if="canViewApplications" @click="resetApplicationQuery">重置</el-button>
            <el-button v-if="hasPermission('FUNDING_APPLICATION_MANAGE')" type="success" @click="openApplicationDialog()">新增资料</el-button>
            <el-button v-if="hasPermission('FUNDING_IMPORT')" type="warning" :loading="importLoading" @click="triggerImport">导入台账</el-button>
            <input ref="importInputRef" class="hidden-input" type="file" accept=".xlsx" @change="handleImportFile" />
          </div>

          <el-table v-if="canViewApplications" v-loading="applicationLoading" :data="applications" border style="width: 100%; min-width: 1180px">
            <el-table-column prop="applicationNo" label="申请单号" width="150" />
            <el-table-column prop="customerName" label="客户" width="110" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="vehicleModel" label="车型" min-width="150" />
            <el-table-column prop="groupLeader" label="组长" width="100" />
            <el-table-column prop="paymentType" label="付款" width="90">
              <template #default="{ row }">{{ paymentTypeLabel(row.paymentType) }}</template>
            </el-table-column>
            <el-table-column label="应收" width="120" align="right">
              <template #default="{ row }"><MoneyText :amount="row.receivableAmount" /></template>
            </el-table-column>
            <el-table-column prop="pickupDate" label="提车日期" width="120" />
            <el-table-column label="状态" width="130" align="center">
              <template #default="{ row }"><el-tag :type="applicationStatusType(row.status)">{{ applicationStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="310" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row.id, 'application')">详情</el-button>
                <el-button v-if="hasPermission('FUNDING_APPLICATION_MANAGE') && canEditApplication(row.status)" link type="primary" @click="openApplicationDialog(row)">编辑</el-button>
                <el-button v-if="hasPermission('FUNDING_APPLICATION_MANAGE') && (row.status === 'DRAFT' || row.status === 'REJECTED')" link type="success" @click="submitApplication(row.id)">提交</el-button>
                <el-button v-if="hasPermission('FUNDING_APPLICATION_AUDIT') && row.status === 'PENDING_AUDIT'" link type="success" @click="approveApplication(row.id)">同意</el-button>
                <el-button v-if="hasPermission('FUNDING_APPLICATION_AUDIT') && row.status === 'PENDING_AUDIT'" link type="danger" @click="rejectApplication(row.id)">不同意</el-button>
                <el-button v-if="hasPermission('FUNDING_CONTRACT_MANAGE') && (row.status === 'CONTRACT_PENDING' || row.status === 'APPROVED')" link type="warning" @click="openContractDialog(row)">合同</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="canViewApplications"
            class="pager"
            layout="total, sizes, prev, pager, next"
            :total="applicationTotal"
            v-model:current-page="applicationQuery.pageNo"
            v-model:page-size="applicationQuery.pageSize"
            @current-change="loadApplications"
            @size-change="loadApplications"
          />
        </el-tab-pane>

        <el-tab-pane v-if="canUseLedgerTab" label="正式台账" name="ledgers">
          <div class="toolbar">
            <el-input v-model="ledgerQuery.keyword" placeholder="搜索台账号、姓名、电话、车型" clearable style="width: 260px" @keyup.enter="loadLedgers" />
            <el-select v-model="ledgerQuery.status" placeholder="状态" clearable style="width: 180px">
              <el-option v-for="item in ledgerStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button v-if="canViewLedgers" type="primary" @click="loadLedgers">查询</el-button>
            <el-button v-if="canViewLedgers" @click="resetLedgerQuery">重置</el-button>
            <el-button v-if="hasPermission('FUNDING_EXPORT')" type="success" :loading="exportLoading" @click="exportLedgers">导出台账</el-button>
          </div>

          <el-table v-if="canViewLedgers" v-loading="ledgerLoading" :data="ledgers" border style="width: 100%; min-width: 1180px">
            <el-table-column prop="ledgerNo" label="台账编号" width="150" />
            <el-table-column prop="customerName" label="客户" width="110" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="vehicleModel" label="车型" min-width="150" />
            <el-table-column label="应收" width="120" align="right">
              <template #default="{ row }"><MoneyText :amount="row.receivableAmount" /></template>
            </el-table-column>
            <el-table-column label="已收" width="120" align="right">
              <template #default="{ row }"><MoneyText :amount="row.receivedAmount" type="success" /></template>
            </el-table-column>
            <el-table-column label="未收" width="120" align="right">
              <template #default="{ row }"><MoneyText :amount="row.outstandingAmount" type="warning" /></template>
            </el-table-column>
            <el-table-column prop="groupLeader" label="组长" width="100" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }"><el-tag :type="ledgerStatusType(row.status)">{{ ledgerStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row.id, 'ledger')">详情</el-button>
                <el-button v-if="hasPermission('FUNDING_LEDGER_MANAGE')" link type="primary" @click="openLedgerDialog(row)">修改</el-button>
                <el-button v-if="hasPermission('FUNDING_PAYMENT_RECORD')" link type="success" @click="openPaymentDialog(row)">收款</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="canViewLedgers"
            class="pager"
            layout="total, sizes, prev, pager, next"
            :total="ledgerTotal"
            v-model:current-page="ledgerQuery.pageNo"
            v-model:page-size="ledgerQuery.pageSize"
            @current-change="loadLedgers"
            @size-change="loadLedgers"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="applicationDialog.visible" :title="applicationDialog.form.id ? '编辑资料' : '新增资料'" width="760px">
      <el-form :model="applicationDialog.form" label-width="110px" class="two-col-form">
        <el-form-item label="客户姓名"><el-input v-model="applicationDialog.form.customerName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="applicationDialog.form.phone" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="applicationDialog.form.idCardNo" /></el-form-item>
        <el-form-item label="车型"><el-input v-model="applicationDialog.form.vehicleModel" /></el-form-item>
        <el-form-item label="提车日期"><el-date-picker v-model="applicationDialog.form.pickupDate" value-format="YYYY-MM-DD" type="date" /></el-form-item>
        <el-form-item label="付款方式">
          <el-select v-model="applicationDialog.form.paymentType">
            <el-option label="全款" value="FULL" />
            <el-option label="分期" value="INSTALLMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="进货成本"><el-input-number v-model="applicationDialog.form.purchaseCost" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="激励"><el-input-number v-model="applicationDialog.form.incentiveAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="上级费用"><el-input-number v-model="applicationDialog.form.upstreamAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="总计成本"><el-input-number v-model="applicationDialog.form.totalCost" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="零售价格"><el-input-number v-model="applicationDialog.form.retailPrice" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="应收总计"><el-input-number v-model="applicationDialog.form.receivableAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="首付"><el-input-number v-model="applicationDialog.form.downPayment" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="分期期数"><el-input-number v-model="applicationDialog.form.installmentCount" :min="0" /></el-form-item>
        <el-form-item label="每期金额"><el-input-number v-model="applicationDialog.form.installmentAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="首期到期日"><el-date-picker v-model="applicationDialog.form.firstDueDate" value-format="YYYY-MM-DD" type="date" /></el-form-item>
        <el-form-item label="组长"><el-input v-model="applicationDialog.form.groupLeader" /></el-form-item>
        <el-form-item label="经办人"><el-input v-model="applicationDialog.form.handlerName" /></el-form-item>
        <el-form-item label="加装备注" class="span-2"><el-input v-model="applicationDialog.form.addOnRemark" /></el-form-item>
        <el-form-item label="备注" class="span-2"><el-input v-model="applicationDialog.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveApplication">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contractDialog.visible" title="线下合同上传确认" width="520px">
      <el-form :model="contractDialog.form" label-width="100px">
        <el-form-item label="合同编号"><el-input v-model="contractDialog.form.contractNo" placeholder="不填则自动生成" /></el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="contractDialog.form.contractType">
            <el-option label="全款合同" value="FULL" />
            <el-option label="分期合同" value="INSTALLMENT" />
            <el-option label="补充协议" value="SUPPLEMENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="签订日期"><el-date-picker v-model="contractDialog.form.signedDate" value-format="YYYY-MM-DD" type="date" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="contractDialog.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contractDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveContract">保存合同</el-button>
        <el-button type="success" :disabled="!contractDialog.contractId" :loading="saving" @click="confirmContract">确认并生成台账</el-button>
        <el-button v-if="contractDialog.contractId" type="danger" :loading="saving" @click="voidContract">作废合同</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ledgerDialog.visible" title="修改台账" width="760px">
      <el-form :model="ledgerDialog.form" label-width="100px" class="two-col-form">
        <el-form-item label="客户姓名"><el-input v-model="ledgerDialog.form.customerName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="ledgerDialog.form.phone" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="ledgerDialog.form.idCardNo" /></el-form-item>
        <el-form-item label="车型"><el-input v-model="ledgerDialog.form.vehicleModel" /></el-form-item>
        <el-form-item label="提车日期"><el-date-picker v-model="ledgerDialog.form.pickupDate" value-format="YYYY-MM-DD" type="date" /></el-form-item>
        <el-form-item label="付款方式">
          <el-select v-model="ledgerDialog.form.paymentType">
            <el-option label="全款" value="FULL" />
            <el-option label="分期" value="INSTALLMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="进货成本"><el-input-number v-model="ledgerDialog.form.purchaseCost" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="激励"><el-input-number v-model="ledgerDialog.form.incentiveAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="上级费用"><el-input-number v-model="ledgerDialog.form.upstreamAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="总计成本"><el-input-number v-model="ledgerDialog.form.totalCost" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="零售价格"><el-input-number v-model="ledgerDialog.form.retailPrice" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="组长"><el-input v-model="ledgerDialog.form.groupLeader" /></el-form-item>
        <el-form-item label="经办人"><el-input v-model="ledgerDialog.form.handlerName" /></el-form-item>
        <el-form-item label="应收"><el-input-number v-model="ledgerDialog.form.receivableAmount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="ledgerDialog.form.status">
            <el-option v-for="item in ledgerStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因" class="span-2"><el-input v-model="ledgerDialog.form.changeRemark" /></el-form-item>
        <el-form-item label="备注" class="span-2"><el-input v-model="ledgerDialog.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ledgerDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLedger">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paymentDialog.visible" title="登记收款" width="520px">
      <el-form :model="paymentDialog.form" label-width="100px">
        <el-form-item label="分期计划">
          <el-select v-model="paymentDialog.form.installmentPlanId" clearable placeholder="不指定">
            <el-option v-for="item in detail.installmentPlans" :key="item.id" :label="`${item.phaseName} 应收 ${item.receivableAmount} 已收 ${item.receivedAmount}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款金额"><el-input-number v-model="paymentDialog.form.amount" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="paymentDialog.form.paymentMethod">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="转账" value="TRANSFER" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款时间"><el-date-picker v-model="paymentDialog.form.paidAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="paymentDialog.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePayment">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detail.visible" title="资方详情" size="48%">
      <template v-if="detail.data">
        <el-descriptions title="申请资料" :column="2" border>
          <el-descriptions-item label="申请单">{{ detail.data.application.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusLabel(detail.data.application.status) }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.data.application.customerName }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ detail.data.application.vehicleModel }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.data.application.auditRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="detail.data.contract" title="合同" :column="2" border class="detail-block">
          <el-descriptions-item label="合同号">{{ detail.data.contract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ contractStatusLabel(detail.data.contract.status) }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ contractTypeLabel(detail.data.contract.contractType) }}</el-descriptions-item>
          <el-descriptions-item label="签订日期">{{ detail.data.contract.signedDate || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="detail.data.ledger" title="台账" :column="2" border class="detail-block">
          <el-descriptions-item label="台账号">{{ detail.data.ledger.ledgerNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ ledgerStatusLabel(detail.data.ledger.status) }}</el-descriptions-item>
          <el-descriptions-item label="应收"><MoneyText :amount="detail.data.ledger.receivableAmount" /></el-descriptions-item>
          <el-descriptions-item label="未收"><MoneyText :amount="detail.data.ledger.outstandingAmount" type="warning" /></el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">分期计划</h3>
        <el-table :data="detail.data.installmentPlans" border size="small">
          <el-table-column prop="phaseName" label="期次" />
          <el-table-column prop="dueDate" label="到期日" />
          <el-table-column label="应收">
            <template #default="{ row }"><MoneyText :amount="row.receivableAmount" /></template>
          </el-table-column>
          <el-table-column label="已收">
            <template #default="{ row }"><MoneyText :amount="row.receivedAmount" type="success" /></template>
          </el-table-column>
          <el-table-column label="状态">
            <template #default="{ row }">{{ installmentStatusLabel(row.status) }}</template>
          </el-table-column>
        </el-table>

        <h3 class="section-title">收款记录</h3>
        <el-table :data="detail.data.payments" border size="small">
          <el-table-column prop="paymentNo" label="收款编号" />
          <el-table-column label="金额">
            <template #default="{ row }"><MoneyText :amount="row.amount" type="success" /></template>
          </el-table-column>
          <el-table-column label="方式">
            <template #default="{ row }">{{ paymentMethodLabel(row.paymentMethod) }}</template>
          </el-table-column>
          <el-table-column prop="paidAt" label="时间" />
        </el-table>

        <h3 class="section-title">附件留存</h3>
        <div v-if="canUploadAttachment" class="upload-line">
          <el-select v-model="uploadForm.ownerType" style="width: 150px">
            <el-option label="申请资料" value="APPLICATION" />
            <el-option label="合同" value="CONTRACT" />
            <el-option label="收款凭证" value="PAYMENT" />
          </el-select>
          <el-select v-model="uploadForm.attachmentType" style="width: 150px">
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="合同" value="CONTRACT" />
            <el-option label="收款凭证" value="PAYMENT_VOUCHER" />
            <el-option label="车辆资料" value="VEHICLE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
          <input type="file" @change="handleFileChange" />
          <el-button type="primary" :disabled="!uploadForm.file" @click="uploadAttachment">上传</el-button>
        </div>
        <el-table :data="detail.data.attachments" border size="small">
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ attachmentTypeLabel(row.attachmentType) }}</template>
          </el-table-column>
          <el-table-column prop="originalFilename" label="文件名" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="downloadAttachment(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>

        <h3 class="section-title">修改日志</h3>
        <el-table :data="detail.data.changeLogs" border size="small">
          <el-table-column label="字段" width="120">
            <template #default="{ row }">{{ changeFieldLabel(row.fieldName) }}</template>
          </el-table-column>
          <el-table-column label="原值">
            <template #default="{ row }">{{ changeValueLabel(row.fieldName, row.oldValue) }}</template>
          </el-table-column>
          <el-table-column label="新值">
            <template #default="{ row }">{{ changeValueLabel(row.fieldName, row.newValue) }}</template>
          </el-table-column>
          <el-table-column prop="operatedAt" label="时间" width="170" />
        </el-table>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import {
  approveFundingApplication,
  confirmFundingContract,
  createFundingApplication,
  downloadFundingAttachment,
  exportFundingLedgers,
  getFundingApplicationDetail,
  getFundingApplications,
  getFundingLedgerDetail,
  getFundingLedgers,
  getFundingSummary,
  importFundingLedgers,
  recordFundingPayment,
  rejectFundingApplication,
  saveFundingContract,
  submitFundingApplication,
  updateFundingApplication,
  updateFundingLedger,
  uploadFundingAttachment,
  voidFundingContract,
} from '@/api/funding';
import { hasPermission } from '@/utils/permission';
import type { FundingApplication, FundingDetail, FundingLedger, SaveFundingApplicationBody } from '@/types/funding';

const activeTab = ref('applications');
const saving = ref(false);
const exportLoading = ref(false);
const importLoading = ref(false);
const applicationLoading = ref(false);
const ledgerLoading = ref(false);
const importInputRef = ref<HTMLInputElement | null>(null);
const applications = ref<FundingApplication[]>([]);
const ledgers = ref<FundingLedger[]>([]);
const applicationTotal = ref(0);
const ledgerTotal = ref(0);
const summary = ref({ applicationCount: 0, pendingAuditCount: 0, ledgerCount: 0, receivableTotal: 0, receivedTotal: 0, outstandingTotal: 0, overduePlanCount: 0 });

const applicationQuery = reactive({ keyword: '', status: '', pageNo: 1, pageSize: 10 });
const ledgerQuery = reactive({ keyword: '', status: '', pageNo: 1, pageSize: 10 });
const applicationStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_AUDIT' },
  { label: '已同意待合同', value: 'CONTRACT_PENDING' },
  { label: '不同意', value: 'REJECTED' },
  { label: '已生成台账', value: 'LEDGER_CREATED' },
];
const ledgerStatusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '部分收款', value: 'PARTIAL_PAID' },
  { label: '已结清', value: 'SETTLED' },
  { label: '逾期', value: 'OVERDUE' },
  { label: '异常', value: 'ABNORMAL' },
  { label: '作废', value: 'VOIDED' },
];

const emptyApplication = (): SaveFundingApplicationBody & { id?: number } => ({
  customerName: '',
  phone: '',
  idCardNo: '',
  vehicleModel: '',
  pickupDate: new Date().toISOString().slice(0, 10),
  paymentType: 'INSTALLMENT',
  purchaseCost: 0,
  incentiveAmount: 0,
  upstreamAmount: 0,
  totalCost: 0,
  retailPrice: 0,
  receivableAmount: 0,
  downPayment: 0,
  installmentCount: 0,
  installmentAmount: 0,
  firstDueDate: '',
  groupLeader: '',
  handlerName: '',
  addOnRemark: '',
  remark: '',
});

const applicationDialog = reactive({ visible: false, form: emptyApplication() });
const contractDialog = reactive({ visible: false, applicationId: 0, contractId: 0, form: { contractNo: '', contractType: 'INSTALLMENT', signedDate: new Date().toISOString().slice(0, 10), remark: '' } });
const ledgerDialog = reactive({ visible: false, form: {} as Partial<FundingLedger> & { changeRemark?: string } });
const paymentDialog = reactive({ visible: false, ledgerId: 0, form: { installmentPlanId: undefined as number | undefined, amount: 0, paymentMethod: 'WECHAT', paidAt: '', remark: '' } });
const detail = reactive({ visible: false, mode: 'application', id: 0, data: null as FundingDetail | null, installmentPlans: [] as FundingDetail['installmentPlans'] });
const uploadForm = reactive({ ownerType: 'APPLICATION', attachmentType: 'ID_CARD', file: null as File | null });
const canUploadAttachment = computed(() =>
  hasPermission('FUNDING_APPLICATION_MANAGE') ||
  hasPermission('FUNDING_CONTRACT_MANAGE') ||
  hasPermission('FUNDING_PAYMENT_RECORD')
);
const canViewApplications = computed(() => hasPermission('FUNDING_APPLICATION_VIEW'));
const canViewLedgers = computed(() =>
  hasPermission('FUNDING_LEDGER_VIEW') ||
  hasPermission('FUNDING_PAYMENT_RECORD')
);
const canViewSummary = canViewLedgers;
const canUseApplicationTab = computed(() =>
  canViewApplications.value ||
  hasPermission('FUNDING_APPLICATION_MANAGE') ||
  hasPermission('FUNDING_APPLICATION_AUDIT') ||
  hasPermission('FUNDING_CONTRACT_MANAGE') ||
  hasPermission('FUNDING_IMPORT')
);
const canUseLedgerTab = computed(() =>
  canViewLedgers.value ||
  hasPermission('FUNDING_EXPORT')
);

const summaryCards = computed(() => [
  { label: '资料申请', value: summary.value.applicationCount },
  { label: '待审核', value: summary.value.pendingAuditCount },
  { label: '正式台账', value: summary.value.ledgerCount },
  { label: '应收总计', value: `￥${Number(summary.value.receivableTotal || 0).toLocaleString()}` },
  { label: '未收金额', value: `￥${Number(summary.value.outstandingTotal || 0).toLocaleString()}` },
  { label: '逾期期数', value: summary.value.overduePlanCount },
]);

onMounted(async () => {
  if (!canUseApplicationTab.value && canUseLedgerTab.value) {
    activeTab.value = 'ledgers';
  }
  await refreshFundingPage();
});

async function loadSummary() {
  if (!canViewSummary.value) return;
  summary.value = await getFundingSummary();
}

async function loadApplications() {
  if (!canViewApplications.value) return;
  applicationLoading.value = true;
  try {
    const page = await getFundingApplications(applicationQuery);
    applications.value = page.records;
    applicationTotal.value = page.total;
  } finally {
    applicationLoading.value = false;
  }
}

async function loadLedgers() {
  if (!canViewLedgers.value) return;
  ledgerLoading.value = true;
  try {
    const page = await getFundingLedgers(ledgerQuery);
    ledgers.value = page.records;
    ledgerTotal.value = page.total;
  } finally {
    ledgerLoading.value = false;
  }
}

function resetApplicationQuery() {
  applicationQuery.keyword = '';
  applicationQuery.status = '';
  applicationQuery.pageNo = 1;
  loadApplications();
}

function resetLedgerQuery() {
  ledgerQuery.keyword = '';
  ledgerQuery.status = '';
  ledgerQuery.pageNo = 1;
  if (canViewLedgers.value) loadLedgers();
}

function handleTabChange() {
  if (activeTab.value === 'applications' && canViewApplications.value) loadApplications();
  if (activeTab.value === 'ledgers' && canViewLedgers.value) loadLedgers();
}

async function refreshFundingPage() {
  const tasks: Promise<unknown>[] = [];
  if (canViewSummary.value) tasks.push(loadSummary());
  if (canViewApplications.value) tasks.push(loadApplications());
  if (canViewLedgers.value) tasks.push(loadLedgers());
  await Promise.all(tasks);
}

function openApplicationDialog(row?: FundingApplication) {
  applicationDialog.form = row ? { ...row } : emptyApplication();
  applicationDialog.visible = true;
}

async function saveApplication() {
  saving.value = true;
  try {
    const form = { ...applicationDialog.form };
    if (form.id) await updateFundingApplication(form.id, form);
    else await createFundingApplication(form);
    ElMessage.success('资料已保存');
    applicationDialog.visible = false;
    await refreshFundingPage();
  } finally {
    saving.value = false;
  }
}

async function submitApplication(id: number) {
  await submitFundingApplication(id);
  ElMessage.success('已提交审核');
  await loadApplications();
}

async function approveApplication(id: number) {
  await approveFundingApplication(id);
  ElMessage.success('已同意，等待合同上传');
  await loadApplications();
}

async function rejectApplication(id: number) {
  const { value } = await ElMessageBox.prompt('请输入不同意原因', '不同意', { inputPattern: /.+/, inputErrorMessage: '原因不能为空' });
  await rejectFundingApplication(id, value);
  ElMessage.success('已标记不同意');
  await loadApplications();
}

async function openContractDialog(row: FundingApplication) {
  contractDialog.applicationId = row.id;
  contractDialog.contractId = 0;
  contractDialog.form = { contractNo: '', contractType: row.paymentType === 'FULL' ? 'FULL' : 'INSTALLMENT', signedDate: new Date().toISOString().slice(0, 10), remark: '' };
  contractDialog.visible = true;
  const data = await getFundingApplicationDetail(row.id);
  if (data.contract) {
    contractDialog.contractId = data.contract.id;
    contractDialog.form = {
      contractNo: data.contract.contractNo,
      contractType: data.contract.contractType,
      signedDate: data.contract.signedDate || '',
      remark: data.contract.remark || '',
    };
  }
}

async function saveContract() {
  saving.value = true;
  try {
    const contract = await saveFundingContract(contractDialog.applicationId, contractDialog.form);
    contractDialog.contractId = contract.id;
    ElMessage.success('合同信息已保存');
    await loadApplications();
  } finally {
    saving.value = false;
  }
}

async function confirmContract() {
  saving.value = true;
  try {
    await confirmFundingContract(contractDialog.contractId);
    ElMessage.success('合同已确认，台账已生成');
    contractDialog.visible = false;
    await refreshFundingPage();
  } finally {
    saving.value = false;
  }
}

async function voidContract() {
  const { value } = await ElMessageBox.prompt('请输入作废原因', '作废合同', { inputPattern: /.+/, inputErrorMessage: '原因不能为空' });
  saving.value = true;
  try {
    await voidFundingContract(contractDialog.contractId, value);
    ElMessage.success('合同已作废');
    contractDialog.visible = false;
    await loadApplications();
  } finally {
    saving.value = false;
  }
}

async function openDetail(id: number, mode: 'application' | 'ledger') {
  detail.mode = mode;
  detail.id = id;
  detail.data = mode === 'application' ? await getFundingApplicationDetail(id) : await getFundingLedgerDetail(id);
  detail.installmentPlans = detail.data.installmentPlans || [];
  detail.visible = true;
}

function openLedgerDialog(row: FundingLedger) {
  ledgerDialog.form = { ...row, changeRemark: '' };
  ledgerDialog.visible = true;
}

async function saveLedger() {
  saving.value = true;
  try {
    await updateFundingLedger(ledgerDialog.form.id!, ledgerDialog.form);
    ElMessage.success('台账已修改');
    ledgerDialog.visible = false;
    await refreshFundingPage();
  } finally {
    saving.value = false;
  }
}

async function openPaymentDialog(row: FundingLedger) {
  paymentDialog.ledgerId = row.id;
  paymentDialog.form = { installmentPlanId: undefined, amount: 0, paymentMethod: 'WECHAT', paidAt: new Date().toISOString().slice(0, 19), remark: '' };
  detail.data = await getFundingLedgerDetail(row.id);
  detail.installmentPlans = detail.data.installmentPlans || [];
  paymentDialog.visible = true;
}

async function savePayment() {
  saving.value = true;
  try {
    await recordFundingPayment(paymentDialog.ledgerId, paymentDialog.form);
    ElMessage.success('收款已登记');
    paymentDialog.visible = false;
    await refreshFundingPage();
  } finally {
    saving.value = false;
  }
}

async function exportLedgers() {
  exportLoading.value = true;
  try {
    await exportFundingLedgers(ledgerQuery);
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
}

function triggerImport() {
  importInputRef.value?.click();
}

async function handleImportFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  importLoading.value = true;
  try {
    const data = new FormData();
    data.append('file', file);
    const result: any = await importFundingLedgers(data);
    ElMessage.success(`导入完成：成功 ${result.successRows || 0} 行，失败 ${result.failedRows || 0} 行`);
    await refreshFundingPage();
  } finally {
    importLoading.value = false;
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  uploadForm.file = input.files?.[0] || null;
}

async function uploadAttachment() {
  if (!uploadForm.file || !detail.data) return;
  let ownerId = detail.data.application.id;
  if (uploadForm.ownerType === 'CONTRACT') ownerId = detail.data.contract?.id || 0;
  if (uploadForm.ownerType === 'PAYMENT') ownerId = detail.data.ledger?.id || 0;
  if (!ownerId) {
    ElMessage.warning('当前详情缺少对应归属对象');
    return;
  }
  const data = new FormData();
  data.append('ownerType', uploadForm.ownerType);
  data.append('ownerId', String(ownerId));
  data.append('attachmentType', uploadForm.attachmentType);
  data.append('file', uploadForm.file);
  await uploadFundingAttachment(data);
  ElMessage.success('附件已上传');
  await openDetail(detail.id, detail.mode as 'application' | 'ledger');
}

async function downloadAttachment(row: { id: number; originalFilename?: string }) {
  await downloadFundingAttachment(row.id, row.originalFilename);
}

function canEditApplication(status: string) {
  return ['DRAFT', 'REJECTED', 'PENDING_AUDIT', 'CONTRACT_PENDING'].includes(status);
}

function paymentTypeLabel(value: string) {
  return value === 'FULL' ? '全款' : value === 'INSTALLMENT' ? '分期' : '未知付款方式';
}

function contractTypeLabel(value: string) {
  const map: Record<string, string> = {
    FULL: '全款合同',
    INSTALLMENT: '分期合同',
    SUPPLEMENT: '补充协议',
    OFFLINE_UPLOAD: '线下合同',
    OTHER: '其他',
  };
  return map[value] || '未知合同类型';
}

function contractStatusLabel(value: string) {
  const map: Record<string, string> = {
    PENDING_UPLOAD: '待上传',
    UPLOADED: '已上传',
    CONFIRMED: '已确认',
    VOIDED: '已作废',
  };
  return map[value] || '未知合同状态';
}

function applicationStatusLabel(value: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_AUDIT: '待审核',
    APPROVED: '已同意',
    REJECTED: '不同意',
    CONTRACT_PENDING: '待合同',
    CONTRACT_CONFIRMED: '合同已确认',
    LEDGER_CREATED: '已生成台账',
    VOIDED: '作废',
  };
  return map[value] || '未知资料状态';
}

function installmentStatusLabel(value: string) {
  const map: Record<string, string> = {
    PENDING: '待收款',
    PARTIAL_PAID: '部分收款',
    PAID: '已收齐',
    OVERDUE: '已逾期',
  };
  return map[value] || '未知收款状态';
}

function ledgerStatusLabel(value: string) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    PARTIAL_PAID: '部分收款',
    SETTLED: '已结清',
    OVERDUE: '逾期',
    ABNORMAL: '异常',
    VOIDED: '作废',
  };
  return map[value] || '未知台账状态';
}

function paymentMethodLabel(value: string) {
  const map: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信',
    ALIPAY: '支付宝',
    TRANSFER: '转账',
    OTHER: '其他',
  };
  return map[value] || '未知收款方式';
}

function attachmentTypeLabel(value: string) {
  const map: Record<string, string> = {
    ID_CARD: '身份证',
    CONTRACT: '合同',
    PAYMENT_VOUCHER: '收款凭证',
    VEHICLE: '车辆资料',
    OTHER: '其他资料',
  };
  return map[value] || '未知附件类型';
}

function changeFieldLabel(value: string) {
  const map: Record<string, string> = {
    customerName: '客户姓名',
    phone: '电话',
    idCardNo: '身份证号',
    vehicleModel: '车型',
    pickupDate: '提车日期',
    paymentType: '付款方式',
    purchaseCost: '进货成本',
    incentiveAmount: '激励',
    upstreamAmount: '上级费用',
    totalCost: '总计成本',
    retailPrice: '零售价格',
    receivableAmount: '应收总计',
    receivedAmount: '已收金额',
    outstandingAmount: '未收金额',
    groupLeader: '组长',
    handlerName: '经办人',
    status: '状态',
    remark: '备注',
  };
  return map[value] || '未知字段';
}

function changeValueLabel(fieldName: string, value?: string | null) {
  if (value === undefined || value === null || value === '') return '-';
  if (fieldName === 'paymentType') return paymentTypeLabel(value);
  if (fieldName === 'status') return ledgerStatusLabel(value);
  return value;
}

function applicationStatusType(value: string) {
  if (value === 'REJECTED' || value === 'VOIDED') return 'danger';
  if (value === 'PENDING_AUDIT' || value === 'CONTRACT_PENDING') return 'warning';
  if (value === 'LEDGER_CREATED' || value === 'CONTRACT_CONFIRMED') return 'success';
  return 'info';
}

function ledgerStatusType(value: string) {
  if (value === 'SETTLED') return 'success';
  if (value === 'PARTIAL_PAID' || value === 'OVERDUE') return 'warning';
  if (value === 'ABNORMAL' || value === 'VOIDED') return 'danger';
  return 'info';
}
</script>

<style scoped>
.summary-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  border-radius: 6px;
}

.summary-label {
  font-size: 13px;
  color: #6b7280;
}

.summary-value {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.main-card {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.pager {
  justify-content: flex-end;
  margin-top: 14px;
}

.two-col-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 12px;
}

.two-col-form :deep(.span-2) {
  grid-column: 1 / span 2;
}

.detail-block {
  margin-top: 18px;
}

.section-title {
  margin: 20px 0 10px;
  font-size: 15px;
  color: #111827;
}

.upload-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.hidden-input {
  display: none;
}
</style>
