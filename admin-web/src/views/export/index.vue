<template>
  <PageContainer title="Excel 导出中心" description="数据导出">
    <el-empty
      description=""
      :image-size="120"
      style="padding: 80px 0;"
    >
      <template #description>
        <div style="text-align: center; color: #6b7280; font-size: 15px; line-height: 2;">
          <div style="font-size: 32px; margin-bottom: 16px;">📥</div>
          <div style="font-weight: 600; color: #374151; margin-bottom: 8px;">导出中心暂未开放</div>
          <div>当前可在以下页面使用各自的导出按钮：</div>
          <div style="margin-top: 12px; display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;">
            <el-tag>财务报表</el-tag>
            <el-tag>报销台账</el-tag>
          </div>
        </div>
      </template>
    </el-empty>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/PageContainer.vue';
</script>
