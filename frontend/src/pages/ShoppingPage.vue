<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>식단 재료 쇼핑 추천</h1>
        <p>
          11번가 API와 연동해서, 식단에 필요한 재료를 검색하고 가격을 비교할 수 있는 화면입니다.
          지금은 키워드 검색과 기본 추천(예: 500g 기준)까지 연결된 상태입니다.
        </p>
      </div>
    </header>

    <!-- 검색 박스 -->
    <NnCard title="재료 검색">
      <ShoppingSearchBar @search="onSearch" />
    </NnCard>

    <!-- 상태 영역: 로딩 / 에러 / 검색 결과 / 추천 결과 -->

    <NnCard title="검색 결과">
      <div v-if="loading" class="page__state">검색 중입니다...</div>

      <div v-else-if="errorMessage" class="page__state page__state--error">
        {{ errorMessage }}
      </div>

      <div v-else-if="products.length === 0" class="page__state">
        검색어를 입력하면 이 아래에 상품 리스트가 보입니다.
      </div>

      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in products"
          :key="p.externalId"
          :product="p"
        />
      </div>
    </NnCard>

    <NnCard title="추천 상품 (예: 500g 기준)">
      <div v-if="recommendLoading" class="page__state">
        추천 상품 계산 중입니다...
      </div>

      <div
        v-else-if="recommendError"
        class="page__state page__state--error"
      >
        {{ recommendError }}
      </div>

      <div v-else-if="recommended.length === 0" class="page__state">
        아직 추천 결과가 없습니다. 위에서 재료를 검색하면 함께 추천됩니다.
      </div>

      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in recommended"
          :key="p.externalId"
          :product="p"
        />
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 상대 경로로 가져오기 (STS + Vite 환경 맞춤)
import NnCard from '../components/common/NnCard.vue';
import ShoppingSearchBar from '../components/shopping/ShoppingSearchBar.vue';
import ShoppingProductCard from '../components/shopping/ShoppingProductCard.vue';

import { searchProducts, recommendProducts } from '../api/shopping';

// 상태 값들
const products = ref([]);
const recommended = ref([]);

const loading = ref(false);
const errorMessage = ref('');

const recommendLoading = ref(false);
const recommendError = ref('');

// 검색 핸들러
const onSearch = async (keyword) => {
  if (!keyword || !keyword.trim()) {
    errorMessage.value = '검색어를 입력해 주세요.';
    return;
  }

  const trimmed = keyword.trim();

  // 초기화
  loading.value = true;
  errorMessage.value = '';
  products.value = [];

  // 추천 쪽도 같이 초기화
  recommendLoading.value = true;
  recommendError.value = '';
  recommended.value = [];

  try {
    // 1) 기본 검색 호출
    const searchRes = await searchProducts(trimmed);
    if (searchRes.success) {
      products.value = searchRes.data || [];
    } else {
      errorMessage.value = searchRes.message || '검색에 실패했습니다.';
    }

    // 2) 추천 API 호출 (예: 500g 기준)
    const recommendRes = await recommendProducts(trimmed, 500);
    if (recommendRes.success) {
      recommended.value = recommendRes.data || [];
    } else {
      recommendError.value =
        recommendRes.message || '추천 상품 조회에 실패했습니다.';
    }
  } catch (err) {
    console.error('[ShoppingPage] 검색/추천 에러', err);
    errorMessage.value = '검색 중 오류가 발생했습니다.';
    recommendError.value = '추천 중 오류가 발생했습니다.';
  } finally {
    loading.value = false;
    recommendLoading.value = false;
  }
};
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

.page__state {
  font-size: 13px;
  color: #6b7280;
}

.page__state--error {
  color: #ef4444;
}

.page__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
