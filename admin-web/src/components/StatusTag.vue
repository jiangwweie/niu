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
  if (['success', 'completed', 'paid', '1', 'part_arrived', 'parts_arrived', 'settled', 'repair_done', 'delivered', 'consumed'].includes(s)) return 'success';
  if (['warning', 'pending', 'unpaid', '2', 'repairing', 'partial_paid', 'refund_pending', 'reserved'].includes(s)) return 'warning';
  if (['danger', 'error', 'failed', 'refunded', '3', 'partial_refunded'].includes(s)) return 'danger';
  if (['info', 'closed', 'canceled', '4', 'draft', 'cancelled', 'no_charge', 'not_reserved', 'released'].includes(s)) return 'info';
  return ''; // primary default
});
</script>
