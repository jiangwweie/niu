<template>
  <div class="page-container">
    <div class="page-header" v-if="title || description">
      <div class="title-section">
        <h2 v-if="title">{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div class="action-section" v-if="$slots.action">
        <slot name="action"></slot>
      </div>
    </div>
    
    <div class="page-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  description?: string;
}>();
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.title-section h2 {
  font-size: 24px;
  font-weight: bold;
  color: #111827;
  margin: 0 0 4px 0;
}

.title-section p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.action-section {
  display: flex;
  gap: 8px;
}

.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
</style>
