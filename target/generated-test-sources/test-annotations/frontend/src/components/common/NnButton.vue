<template>
  <button
    :type="type"
    class="nn-button"
    :class="[
      `nn-button--${variant}`,
      { 'nn-button--block': block },
    ]"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup>
const props = defineProps({
  type: {
    type: String,
    default: 'button',
  },
  variant: {
    type: String,
    default: 'primary', // primary, secondary 정도만
  },
  block: {
    type: Boolean,
    default: false,
  },
});
</script>

<style scoped>
.nn-button {
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

/* 메인 액션 버튼 */
.nn-button--primary {
  background-color: #4f46e5;
  color: #ffffff;
}

.nn-button--primary:hover {
  background-color: #4338ca;
}

/* 서브 액션 버튼 */
.nn-button--secondary {
  background-color: #e5e7eb;
  color: #111827;
}

.nn-button--secondary:hover {
  background-color: #d1d5db;
}

.nn-button--block {
  width: 100%;
}
</style>
