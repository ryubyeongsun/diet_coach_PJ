<script setup>
import { ref } from 'vue';
import NnInput from '../common/NnInput.vue';
import NnButton from '../common/NnButton.vue';

const emit = defineEmits(['search']);

const keyword = ref('닭가슴살');

const doSearch = () => {
  if (!keyword.value.trim()) return;
  emit('search', keyword.value.trim());
};
</script>

<template>
  <div class="search-bar">
    <NnInput
      v-model="keyword"
      placeholder="예: 닭가슴살, 현미밥, 샐러드 드레싱..."
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
