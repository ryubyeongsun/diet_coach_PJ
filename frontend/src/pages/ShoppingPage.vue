<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>식단 재료 쇼핑 추천</h1>
        <p>
          11번가 API와 연동해서 식단에 필요한 재료를 검색하고,
          가격 비교용 추천 상품을 보여주는 화면입니다.
        </p>
      </div>
    </header>

    <NnCard title="재료 검색">
      <ShoppingSearchBar @search="onSearch" />
      <p v-if="errorMessage" class="page__error">
        {{ errorMessage }}
      </p>
    </NnCard>

    <NnCard title="검색 결과">
      <div v-if="isLoading" class="page__status">
        검색 중입니다...
      </div>

      <div v-else-if="searchResults.length === 0">
        검색어를 입력하면 이 아래에 상품 리스트가 표시됩니다.
      </div>

      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in searchResults"
          :key="p.externalId"
          :product="p"
        />
      </div>
    </NnCard>

    <NnCard title="추천 상품 (테스트: 500g 기준)">
      <div v-if="isLoadingReco" class="page__status">
        추천 상품을 불러오는 중입니다...
      </div>

      <div v-else-if="recommendations.length === 0">
        아직 추천 결과가 없습니다. 위에서 검색을 먼저 해보세요.
      </div>

      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in recommendations"
          :key="`reco-${p.externalId}`"
          :product="p"
        />
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref } from 'vue';

import NnCard from '../components/common/NnCard.vue';
import ShoppingSearchBar from '../components/shopping/ShoppingSearchBar.vue';
import ShoppingProductCard from '../components/shopping/ShoppingProductCard.vue';
import { searchProducts, getRecommendations } from '../api/shoppingApi.js';

const searchResults = ref([]);
const recommendations = ref([]);
const isLoading = ref(false);
const isLoadingReco = ref(false);
const errorMessage = ref('');

async function onSearch(keyword) {
  if (!keyword || !keyword.trim()) {
    errorMessage.value = '검색어를 입력해 주세요.';
    return;
  }

  isLoading.value = true;
  isLoadingReco.value = true;
  errorMessage.value = '';
  searchResults.value = [];
  recommendations.value = [];

  try {
    // 1) 일반 검색
    searchResults.value = await searchProducts(keyword);

    // 2) 추천 (일단 neededGram은 500g 고정으로 테스트)
    recommendations.value = await getRecommendations(keyword, 500);
  } catch (err) {
    console.error(err);
    errorMessage.value = '검색 중 오류가 발생했습니다.';
  } finally {
    isLoading.value = false;
    isLoadingReco.value = false;
  }
}
</script>

<style scoped>
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page__header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.page__header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.page__error {
  margin-top: 8px;
  font-size: 13px;
  color: #dc2626;
}

.page__status {
  font-size: 13px;
  color: #6b7280;
}

.page__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
