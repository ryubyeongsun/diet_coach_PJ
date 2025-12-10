<script setup>
import { computed } from 'vue';
import NnInput from '../common/NnInput.vue';
import NnButton from '../common/NnButton.vue';

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:modelValue', 'search']);

const keyword = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const doSearch = () => {
  if (!keyword.value.trim()) return;
  emit('search');
};
</script>

<template>
  <div class="search-bar">
    <NnInput
      v-model="keyword"
      placeholder="예: 닭가슴살, 현미밥, 샐러드 드레싱..."
      @keyup.enter="doSearch"
    />
    <NnButton @click="doSearch">
      검색
    </NnButton>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  gap: 8px;
}
.search-bar button {
  min-width: 72px;
}
</style>
