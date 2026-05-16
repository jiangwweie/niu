<template>
  <PageContainer title="用户与权限" description="账号与权限管理">
    <el-empty
      description=""
      :image-size="120"
      style="padding: 80px 0;"
    >
      <template #description>
        <div style="text-align: center; color: #6b7280; font-size: 15px; line-height: 2;">
          <div style="font-size: 32px; margin-bottom: 16px;">🔒</div>
          <div style="font-weight: 600; color: #374151; margin-bottom: 8px;">用户与权限管理暂未开放</div>
          <div>当前账号与权限由系统初始化和后端 RBAC 控制。</div>
          <div>如需调整权限，请联系系统管理员。</div>
        </div>
      </template>
    </el-empty>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '@/components/PageContainer.vue';
</script>
