<template>
  <span class="money-text" :class="[bold ? 'is-bold' : '', type ? `text-${type}` : '']">
    <span class="currency-symbol">{{ symbol }}</span>
    <span class="amount">{{ formattedAmount }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  amount: number | string;
  symbol?: string;
  bold?: boolean;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}>(), {
  symbol: '￥',
  bold: false
});

const formattedAmount = computed(() => {
  const num = Number(props.amount);
  if (isNaN(num)) return '0.00';
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
});
</script>

<style scoped>
.money-text {
  display: inline-flex;
  align-items: baseline;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.is-bold {
  font-weight: 600;
}

.currency-symbol {
  font-size: 0.85em;
  margin-right: 2px;
}

.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.text-info { color: #909399; }
</style>
