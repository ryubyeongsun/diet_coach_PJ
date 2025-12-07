<template>
  <div class="nn-card">
    <div v-if="title" class="nn-card__header">
      <h3 class="nn-card__title">{{ title }}</h3>
      <slot name="header-extra" />
    </div>
    <div class="nn-card__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="nn-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: '',
  },
});
</script>

<style scoped>
.nn-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
  border: 1px solid #e5e7eb;
}

.nn-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.nn-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.nn-card__body {
  font-size: 14px;
  color: #374151;
}

.nn-card__footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}
</style>
