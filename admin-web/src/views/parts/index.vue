<template>
  <PageContainer title="配件管理" description="维护官方配件、第三方配件与基础物料信息">

    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="80px" class="search-form-flex" size="default">
        <el-form-item label="配件编码">
          <el-input v-model="queryParams.partCode" placeholder="支持模糊匹配" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="配件名称">
          <el-input v-model="queryParams.partName" placeholder="请输入配件名称" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="官方品号">
          <el-input v-model="queryParams.officialCode" placeholder="支持模糊匹配" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="queryParams.model" placeholder="请输入型号" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="queryParams.category" placeholder="请输入分类" clearable style="width: 220px;" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="queryParams.source" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="官方" value="OFFICIAL" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 220px;">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span class="table-title">配件资料列表</span>
        </div>
        <div class="toolbar-right">
          <el-button v-if="hasPermission('PART_MANAGE')" type="success" @click="openAddDialog">新增配件</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          style="width: 100%; min-width: 1000px"
          border
        >
          <el-table-column prop="partCode" label="配件编码" width="140" />
          <el-table-column prop="partName" label="配件名称" min-width="150" show-overflow-tooltip />
          <el-table-column label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="isOfficialSource(row.source) ? 'danger' : 'info'" size="small">
                {{ isOfficialSource(row.source) ? '官方' : '第三方' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="官方品号" width="130">
            <template #default="{ row }">
              {{ row.officialCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="model" label="型号" width="100" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="成本价" width="100" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.costPrice" />
            </template>
          </el-table-column>
          <el-table-column label="销售价" width="100" align="right">
            <template #default="{ row }">
              <MoneyText v-if="row.salePrice != null" :amount="row.salePrice" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="barcode" label="系统条码" width="160" />
          <el-table-column prop="location" label="库存位置" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status ? 'success' : 'info'" size="small">
                {{ row.status ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button v-if="hasPermission('PART_MANAGE')" link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="primary" @click="printBarcode(row)">打印条码</el-button>
              <el-button v-if="hasPermission('PART_MANAGE')" link :type="row.status ? 'danger' : 'success'" @click="handleToggleStatus(row)">
                {{ row.status ? '停用' : '启用' }}
              </el-button>
              <el-dropdown v-if="hasPermission('PART_MANAGE')" trigger="click" @command="(command: string) => handleAdvancedCommand(command, row)">
                <el-button link type="warning">高级操作</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="delete-check">查看删除校验</el-dropdown-item>
                    <el-dropdown-item command="inventory-flows">查看库存流水</el-dropdown-item>
                    <el-dropdown-item command="work-orders">查看关联工单</el-dropdown-item>
                    <el-dropdown-item command="delete">安全删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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

    <!-- 配件详情弹窗 -->
    <el-drawer v-model="detailDrawer.visible" title="配件详情" size="560px">
      <div v-if="detailDrawer.data">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="配件编码">{{ detailDrawer.data.partCode }}</el-descriptions-item>
          <el-descriptions-item label="配件名称">{{ detailDrawer.data.partName }}</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag :type="isOfficialSource(detailDrawer.data.source) ? 'danger' : 'info'" size="small">
              {{ isOfficialSource(detailDrawer.data.source) ? '官方' : '第三方' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="官方品号">{{ detailDrawer.data.officialCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detailDrawer.data.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detailDrawer.data.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成本价"><MoneyText :amount="detailDrawer.data.costPrice" /></el-descriptions-item>
          <el-descriptions-item label="销售价">
            <MoneyText v-if="detailDrawer.data.salePrice != null" :amount="detailDrawer.data.salePrice" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="系统条码">
            <span>{{ detailDrawer.data.barcode || '-' }}</span>
            <el-button
              v-if="detailDrawer.data.barcode"
              link
              type="primary"
              class="inline-action"
              @click="copyBarcode(detailDrawer.data.barcode)"
            >复制</el-button>
            <el-button
              v-if="detailDrawer.data.barcode"
              link
              type="primary"
              class="inline-action"
              @click="printBarcode(detailDrawer.data)"
            >打印</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="库存位置" :span="2">{{ detailDrawer.data.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailDrawer.data.status ? 'success' : 'info'" size="small">
              {{ detailDrawer.data.status ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailDrawer.data.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 新增配件弹窗 -->
    <el-dialog v-model="editDialog.visible" :title="editDialog.isEdit ? '编辑配件' : '新增配件'" width="600px">
      <el-form :model="editDialog.form" label-width="100px" size="default">
        <el-form-item label="配件来源" v-if="!editDialog.isEdit">
          <el-radio-group v-model="editDialog.form.source">
            <el-radio label="official">官方配件</el-radio>
            <el-radio label="third_party">第三方配件</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="配件名称" required>
          <el-input v-model="editDialog.form.partName" placeholder="请输入配件名称" />
        </el-form-item>
        <el-form-item label="官方品号" v-if="editDialog.form.source === 'official'" required>
          <el-input v-model="editDialog.form.officialPartNo" placeholder="请输入官方品号" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="editDialog.form.model" placeholder="请输入适用型号，如 NQi, 通用" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="editDialog.form.categoryCode" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="editDialog.form.referenceCostPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="销售价">
          <el-input-number v-model="editDialog.form.defaultSalePrice" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="系统条码">
          <el-input
            v-model="editDialog.form.defaultBarcode"
            placeholder="留空由系统自动生成"
          />
        </el-form-item>
        <el-form-item label="外部条码">
          <el-input
            v-model="editDialog.form.externalBarcode"
            placeholder="选填，用于绑定包装条码/官方条码"
          />
        </el-form-item>
        <el-form-item label="库存位置">
          <el-input v-model="editDialog.form.locationRemark" placeholder="例如：A区-01架-02层" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editDialog.form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deleteCheckDialog.visible" title="删除校验" width="640px">
      <template v-if="deleteCheckDialog.data">
        <el-alert
          :title="deleteCheckDialog.data.canDelete ? '该配件满足安全删除条件' : '该配件当前不能删除'"
          :type="deleteCheckDialog.data.canDelete ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />
        <el-descriptions :column="3" border size="small" class="delete-check-section">
          <el-descriptions-item label="实际库存">{{ deleteCheckDialog.data.stockSummary.actualQty }}</el-descriptions-item>
          <el-descriptions-item label="可用库存">{{ deleteCheckDialog.data.stockSummary.availableQty }}</el-descriptions-item>
          <el-descriptions-item label="预占库存">{{ deleteCheckDialog.data.stockSummary.reservedQty }}</el-descriptions-item>
          <el-descriptions-item label="库存流水">{{ deleteCheckDialog.data.referenceSummary.inventoryFlowCount }}</el-descriptions-item>
          <el-descriptions-item label="工单引用">{{ deleteCheckDialog.data.referenceSummary.workOrderChargeItemCount }}</el-descriptions-item>
          <el-descriptions-item label="关联工单示例">
            <span v-if="deleteCheckDialog.data.referenceSummary.sampleWorkOrderIds.length">
              {{ deleteCheckDialog.data.referenceSummary.sampleWorkOrderIds.join('、') }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="delete-check-section">
          <div class="section-title">删除原因</div>
          <el-empty v-if="!deleteCheckDialog.data.reasons.length" description="当前没有删除阻塞原因" :image-size="80" />
          <el-tag
            v-for="reason in deleteCheckDialog.data.reasons"
            :key="reason"
            type="warning"
            class="reason-tag"
          >
            {{ reason }}
          </el-tag>
        </div>
      </template>
      <template #footer>
        <el-button @click="deleteCheckDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import JsBarcode from 'jsbarcode';
import { useRouter } from 'vue-router';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { hasPermission } from '@/utils/permission';
import {
  getPartsList,
  getPartDetail,
  createOfficialPart,
  createThirdPartyPart,
  updatePart,
  enablePart,
  disablePart,
  deletePart,
  getPartDeleteCheck,
} from '@/api/parts';
import type { PartViewRecord } from '@/api/parts';

const router = useRouter();

// 查询参数
const queryParams = reactive({
  partCode: '',
  partName: '',
  officialCode: '',
  model: '',
  category: '',
  source: '' as string,
  status: '' as boolean | string,
  pageNo: 1,
  pageSize: 10,
});

const loading = ref(false);
const tableData = ref<PartViewRecord[]>([]);
const total = ref(0);

const isOfficialSource = (source?: string) => String(source || '').toUpperCase() === 'OFFICIAL';

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPartsList(queryParams);
    tableData.value = res.records;
    total.value = res.total;
  } catch {
    ElMessage.error('加载配件列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.partCode = '';
  queryParams.partName = '';
  queryParams.officialCode = '';
  queryParams.model = '';
  queryParams.category = '';
  queryParams.source = '';
  queryParams.status = '';
  handleSearch();
};

// 配件详情抽屉
const detailDrawer = reactive({
  visible: false,
  data: null as PartViewRecord | null,
});

const deleteCheckDialog = reactive({
  visible: false,
  loading: false,
  partName: '',
  data: null as null | {
    canDelete: boolean;
    reasons: string[];
    stockSummary: {
      actualQty: number;
      availableQty: number;
      reservedQty: number;
    };
    referenceSummary: {
      inventoryFlowCount: number;
      workOrderChargeItemCount: number;
      sampleWorkOrderIds: number[];
    };
  },
});

const handleView = async (row: PartViewRecord) => {
  try {
    const detail = await getPartDetail(row.id);
    detailDrawer.data = detail;
    detailDrawer.visible = true;
  } catch {
    ElMessage.error('加载配件详情失败');
  }
};

// 新增 / 编辑配件弹窗
const editDialog = reactive({
  visible: false,
  isEdit: false,
  saving: false,
  editPartId: '',
  form: {
    source: 'official',
    partName: '',
    officialPartNo: '',
    model: '',
    categoryCode: '',
    referenceCostPrice: 0,
    defaultSalePrice: null as number | null,
    defaultBarcode: '',
    externalBarcode: '',
    locationRemark: '',
    remark: '',
  },
});

const openAddDialog = () => {
  editDialog.isEdit = false;
  editDialog.editPartId = '';
  editDialog.form = {
    source: 'official',
    partName: '',
    officialPartNo: '',
    model: '',
    categoryCode: '',
    referenceCostPrice: 0,
    defaultSalePrice: null as number | null,
    defaultBarcode: '',
    externalBarcode: '',
    locationRemark: '',
    remark: '',
  };
  editDialog.visible = true;
};

const openEditDialog = (row: PartViewRecord) => {
  editDialog.isEdit = true;
  editDialog.editPartId = row.id;
  editDialog.form = {
    source: row.source,
    partName: row.partName,
    officialPartNo: row.officialCode,
    model: row.model,
    categoryCode: row.category,
    referenceCostPrice: row.costPrice,
    defaultSalePrice: row.salePrice,
    defaultBarcode: row.barcode,
    externalBarcode: '',
    locationRemark: row.location,
    remark: row.remark,
  };
  editDialog.visible = true;
};

const submitEdit = async () => {
  if (!editDialog.form.partName) {
    ElMessage.warning('请输入配件名称');
    return;
  }

  editDialog.saving = true;
  try {
    if (editDialog.isEdit) {
      if (editDialog.form.source === 'official' && !editDialog.form.officialPartNo) {
        ElMessage.warning('官方配件必须填写品号');
        editDialog.saving = false;
        return;
      }
      await updatePart(editDialog.editPartId, {
        partName: editDialog.form.partName,
        officialPartNo: editDialog.form.officialPartNo || undefined,
        model: editDialog.form.model || undefined,
        categoryCode: editDialog.form.categoryCode || undefined,
        referenceCostPrice: editDialog.form.referenceCostPrice || undefined,
        defaultSalePrice: editDialog.form.defaultSalePrice ?? undefined,
        defaultBarcode: editDialog.form.defaultBarcode || undefined,
        externalBarcode: editDialog.form.externalBarcode || undefined,
        locationRemark: editDialog.form.locationRemark || undefined,
        remark: editDialog.form.remark || undefined,
      });
      ElMessage.success('配件更新成功');
    } else {
      if (editDialog.form.source === 'official') {
        if (!editDialog.form.officialPartNo) {
          ElMessage.warning('官方配件必须填写品号');
          editDialog.saving = false;
          return;
        }
        await createOfficialPart({
          partName: editDialog.form.partName,
          officialPartNo: editDialog.form.officialPartNo,
          model: editDialog.form.model || undefined,
          categoryCode: editDialog.form.categoryCode || undefined,
          referenceCostPrice: editDialog.form.referenceCostPrice || undefined,
          defaultSalePrice: editDialog.form.defaultSalePrice ?? undefined,
          defaultBarcode: editDialog.form.defaultBarcode || undefined,
          externalBarcode: editDialog.form.externalBarcode || undefined,
          locationRemark: editDialog.form.locationRemark || undefined,
          remark: editDialog.form.remark || undefined,
        });
      } else {
        await createThirdPartyPart({
          partName: editDialog.form.partName,
          model: editDialog.form.model || undefined,
          categoryCode: editDialog.form.categoryCode || undefined,
          referenceCostPrice: editDialog.form.referenceCostPrice || undefined,
          defaultSalePrice: editDialog.form.defaultSalePrice ?? undefined,
          defaultBarcode: editDialog.form.defaultBarcode || undefined,
          externalBarcode: editDialog.form.externalBarcode || undefined,
          locationRemark: editDialog.form.locationRemark || undefined,
          remark: editDialog.form.remark || undefined,
        });
      }
      ElMessage.success('配件创建成功');
    }
    editDialog.visible = false;
    fetchData();
  } catch {
    // Error already shown by request interceptor
  } finally {
    editDialog.saving = false;
  }
};

const handleToggleStatus = async (row: PartViewRecord) => {
  const action = row.status ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确定要 ${action} 配件【${row.partName}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    if (row.status) {
      await disablePart(row.id);
    } else {
      await enablePart(row.id);
    }
    ElMessage.success(`已${action}`);
    fetchData();
  } catch {
    // User cancelled or API error (already shown by interceptor)
  }
};

const handleDelete = async (row: PartViewRecord) => {
  const deleteCheck = await showDeleteCheck(row, false);
  if (!deleteCheck) {
    return;
  }
  if (!deleteCheck.canDelete) {
    // If it cannot be deleted, show the check dialog to explain why
    deleteCheckDialog.visible = true;
    return;
  }
  try {
    await ElMessageBox.confirm(
      [
        `配件：${row.partName}`,
        `实际库存：${deleteCheck.stockSummary.actualQty}`,
        `可用库存：${deleteCheck.stockSummary.availableQty}`,
        `预占库存：${deleteCheck.stockSummary.reservedQty}`,
        `库存流水：${deleteCheck.referenceSummary.inventoryFlowCount}`,
        `工单引用：${deleteCheck.referenceSummary.workOrderChargeItemCount}`,
        '删除后该配件将从新业务选件、扫码 lookup、库存默认列表中消失。',
      ].join('\n'),
      `删除配件【${row.partName}】`,
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );
    await deletePart(row.id);
    ElMessage.success('配件已删除');
    fetchData();
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      const msg = err?.response?.data?.message || err?.message || '删除失败';
      ElMessage.error(msg);
    }
  }
};

const showDeleteCheck = async (row: PartViewRecord, showDialog = true) => {
  try {
    deleteCheckDialog.loading = true;
    deleteCheckDialog.partName = row.partName;
    const data = await getPartDeleteCheck(row.id);
    deleteCheckDialog.data = data;
    if (showDialog) {
      deleteCheckDialog.visible = true;
    }
    return data;
  } catch {
    return null;
  } finally {
    deleteCheckDialog.loading = false;
  }
};

const handleAdvancedCommand = async (command: string, row: PartViewRecord) => {
  if (command === 'delete-check') {
    await showDeleteCheck(row);
    return;
  }
  if (command === 'inventory-flows') {
    router.push({
      name: 'Inventory',
      query: {
        view: 'ALL',
        partCode: row.partCode,
        partId: String(row.id),
        openLogs: '1',
      },
    });
    return;
  }
  if (command === 'work-orders') {
    router.push({
      name: 'WorkOrder',
      query: {
        partId: String(row.id),
      },
    });
    return;
  }
  if (command === 'delete') {
    await handleDelete(row);
  }
};

const copyBarcode = async (barcode: string) => {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(barcode);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = barcode;
      textarea.style.position = 'fixed';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }
    ElMessage.success('条码已复制');
  } catch {
    ElMessage.error('复制失败，请手动复制');
  }
};

const escapeHtml = (value: string) => value
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;');

const printBarcode = (row: PartViewRecord) => {
  if (!row.barcode) {
    ElMessage.warning('当前配件缺少系统条码，请刷新列表后重试');
    return;
  }
  if (!row.partCode) {
    ElMessage.warning('当前配件缺少配件编码，无法打印');
    return;
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  try {
    JsBarcode(svg, row.barcode, {
      format: 'CODE128',
      displayValue: true,
      fontSize: 16,
      textMargin: 6,
      width: 2,
      height: 72,
      margin: 0,
    });
  } catch {
    ElMessage.error('条码内容无法生成 Code128');
    return;
  }
  const popup = window.open('', '_blank', 'width=420,height=320');
  if (!popup) {
    ElMessage.error('浏览器阻止了打印窗口');
    return;
  }
  const barcodeSvg = svg.outerHTML;
  popup.document.write(`
    <html>
      <head>
        <title>打印条码</title>
        <style>
          @page { size: 70mm 35mm; margin: 4mm; }
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 0; padding: 8px; }
          .label { border: 1px solid #111; box-sizing: border-box; padding: 8px 10px; width: 62mm; min-height: 27mm; }
          .name { font-size: 14px; font-weight: 600; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
          .code { font-size: 11px; color: #555; margin-bottom: 4px; }
          .barcode-svg { display: block; width: 100%; }
          .barcode-svg svg { width: 100%; height: auto; }
        </style>
      </head>
      <body>
        <div class="label">
          <div class="name">${escapeHtml(row.partName)}</div>
          <div class="code">${escapeHtml(row.partCode)}</div>
          ${row.officialCode ? `<div class="code">${escapeHtml(row.officialCode)}</div>` : ''}
          ${row.model ? `<div class="code">${escapeHtml(row.model)}</div>` : ''}
          <div class="code">${escapeHtml(row.barcode)}</div>
          <div class="barcode-svg">${barcodeSvg}</div>
        </div>
        <script>window.onload = function () { window.print(); };<\/script>
      </body>
    </html>
  `);
  popup.document.close();
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
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
.inline-action {
  margin-left: 8px;
}
.delete-check-section {
  margin-top: 16px;
}
.section-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.reason-tag {
  margin-right: 8px;
  margin-bottom: 8px;
  white-space: normal;
  height: auto;
  line-height: 1.5;
  padding: 6px 10px;
}
</style>
