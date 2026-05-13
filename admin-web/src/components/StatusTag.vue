<template>
  <el-tag :type="tagType" :effect="effect" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  status?: string | number;
  label: string;
  effect?: 'dark' | 'light' | 'plain';
}>(), {
  status: 'default',
  effect: 'light'
});

const tagType = computed(() => {
  const s = String(props.status).toLowerCase();
  if (['success', 'completed', 'paid', '1', 'part_arrived', 'parts_arrived', 'settled'].includes(s)) return 'success';
  if (['warning', 'pending', 'unpaid', '2'].includes(s)) return 'warning';
  if (['danger', 'error', 'failed', 'refunded', '3'].includes(s)) return 'danger';
  if (['info', 'closed', 'canceled', '4'].includes(s)) return 'info';
  return ''; // primary default
});
</script>
