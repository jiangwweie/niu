<template>
  <PageContainer title="支付记录" description="查看客户付款明细，支持多次付款与混合付款记录展示">
    
    <!-- 查询过滤区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="支付编号">
          <el-input v-model="queryParams.paymentNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="工单编号">
          <el-input v-model="queryParams.orderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户姓名">
          <el-input v-model="queryParams.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="queryParams.method" placeholder="全部" clearable style="width: 120px">
            <el-option label="微信支付" value="wechat" />
            <el-option label="支付宝支付" value="alipay" />
            <el-option label="银联支付" value="unionpay" />
            <el-option label="现金支付" value="cash" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款人">
          <el-input v-model="queryParams.payee" placeholder="请输入" clearable />
        </el-form-item>
        <!-- 支付日期范围如果需要可以加上 date-picker -->
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openAddDialog">新增支付记录</el-button>
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
        <el-table-column prop="paymentNo" label="支付编号" width="160" />
        <el-table-column prop="orderNo" label="工单编号" width="160" />
        <el-table-column prop="customerName" label="客户姓名" width="110" />
        <el-table-column label="支付金额" width="120" align="right">
          <template #default="{ row }">
            <MoneyText :amount="row.amount" />
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getMethodTag(row.method)" size="small">
              {{ getMethodLabel(row.method) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentTime" label="支付时间" width="160" />
        <el-table-column prop="payee" label="收款人" width="100" />
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
</div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageNoSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="viewDrawer.visible" title="支付详情" size="500px">
      <el-descriptions v-if="viewDrawer.current" :column="1" border>
        <el-descriptions-item label="支付编号">{{ viewDrawer.current.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="工单编号">{{ viewDrawer.current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ viewDrawer.current.customerName }}</el-descriptions-item>
        <el-descriptions-item label="支付金额"><MoneyText :amount="viewDrawer.current.amount" /></el-descriptions-item>
        <el-descriptions-item label="支付方式">
          <el-tag :type="getMethodTag(viewDrawer.current.method)" size="small">
            {{ getMethodLabel(viewDrawer.current.method) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ viewDrawer.current.paymentTime }}</el-descriptions-item>
        <el-descriptions-item label="收款人">{{ viewDrawer.current.payee }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewDrawer.current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewDrawer.current.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 新增支付记录 -->
    <el-dialog v-model="addDialog.visible" title="新增支付记录 (Mock)" width="500px">
      <el-alert
        title="支付记录只表示客户付款明细，支付完成不等于工单已结算。工单是否允许结算，最终以后端校验为准。"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />
      <el-form :model="addDialog.form" label-width="100px" size="default">
        <el-form-item label="工单编号" required>
          <el-input v-model="addDialog.form.orderNo" placeholder="请输入关联工单编号" />
        </el-form-item>
        <el-form-item label="客户姓名" required>
          <el-input v-model="addDialog.form.customerName" placeholder="请输入客户姓名" />
        </el-form-item>
        <el-form-item label="支付金额" required>
          <el-input-number v-model="addDialog.form.amount" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="支付方式" required>
          <el-select v-model="addDialog.form.method" style="width: 100%">
            <el-option label="微信支付" value="wechat" />
            <el-option label="支付宝支付" value="alipay" />
            <el-option label="银联支付" value="unionpay" />
            <el-option label="现金支付" value="cash" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付时间">
          <el-date-picker v-model="addDialog.form.time" type="datetime" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款人">
          <el-input v-model="addDialog.form.payee" placeholder="请输入收款人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">保存 mock</el-button>
      </template>
    </el-dialog>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getPaymentList } from '@/api/payment';
import type { PaymentQuery, PaymentRecord } from '@/types/payment';

const queryParams = reactive<PaymentQuery>({
  page: 1,
  pageSize: 10,
  paymentNo: '',
  orderNo: '',
  customerName: '',
  method: '',
  payee: ''
});

const loading = ref(false);
const tableData = ref<PaymentRecord[]>([]);
const total = ref(0);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPaymentList(queryParams);
    if (res.code === 'SUCCESS') {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.paymentNo = '';
  queryParams.orderNo = '';
  queryParams.customerName = '';
  queryParams.method = '';
  queryParams.payee = '';
  handleSearch();
};

// 标签与文字映射
const getMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    wechat: '微信',
    alipay: '支付宝',
    unionpay: '银联',
    cash: '现金'
  };
  return map[method] || method;
};

const getMethodTag = (method: string) => {
  const map: Record<string, string> = {
    wechat: 'success',
    alipay: 'primary',
    unionpay: 'warning',
    cash: 'info'
  };
  return map[method] || '';
};

// 详情
const viewDrawer = reactive({
  visible: false,
  current: null as PaymentRecord | null
});

const handleView = (row: PaymentRecord) => {
  viewDrawer.current = row;
  viewDrawer.visible = true;
};

// 新增
const addDialog = reactive({
  visible: false,
  form: {
    orderNo: '',
    customerName: '',
    amount: 0,
    method: 'wechat',
    time: new Date(),
    payee: '店长',
    remark: ''
  }
});

const openAddDialog = () => {
  addDialog.form = {
    orderNo: '',
    customerName: '',
    amount: 0,
    method: 'wechat',
    time: new Date(),
    payee: '店长',
    remark: ''
  };
  addDialog.visible = true;
};

const submitAdd = () => {
  ElMessage.success('mock 支付记录已填写，真实保存以后端接口为准。');
  addDialog.visible = false;
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
</style>
