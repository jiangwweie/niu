<template>
  <PageContainer title="无权限访问" description="当前账号无权查看该页面">
    <el-empty description="当前账号无权查看该页面">
      <template #description>
        <div class="access-denied-copy">
          <div class="title">当前账号无权查看该页面</div>
          <div class="desc">请切换到具备相应权限的员工账号，或联系门店管理员调整角色。</div>
        </div>
      </template>
      <el-button type="primary" @click="goHome">返回可用页面</el-button>
    </el-empty>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import PageContainer from '@/components/PageContainer.vue';

const router = useRouter();

function goHome() {
  router.push('/');
}
</script>

<style scoped>
.access-denied-copy {
  line-height: 1.8;
}

.title {
  color: #1f2937;
  font-weight: 600;
}

.desc {
  color: #6b7280;
  font-size: 14px;
}
</style>
