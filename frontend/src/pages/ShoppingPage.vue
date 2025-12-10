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

    <NnCard title="이 식단에 필요한 재료" v-if="planId">
      <div v-if="isLoadingIngredients" class="page__status">
        재료 정보를 불러오는 중입니다...
      </div>
      <div v-else-if="ingredientsError" class="page__error">
        {{ ingredientsError }}
      </div>
      <div v-else-if="ingredients.length === 0">
        이 식단에 대한 재료 정보가 아직 없습니다.
      </div>
      <ul v-else class="ingredients-list">
        <li
          v-for="ing in ingredients"
          :key="ing.ingredient"
          class="ingredients-list__item"
        >
          <div class="ingredients-list__info">
            <strong>{{ ing.ingredient }}</strong>
            <span v-if="ing.neededGram">
              · 총 {{ ing.neededGram }} g
            </span>
          </div>
          <NnButton
            size="sm"
            @click="searchFromIngredient(ing.ingredient)"
          >
            이 재료 상품 찾기
          </NnButton>
        </li>
      </ul>
    </NnCard>

    <NnCard title="재료 검색">
      <ShoppingSearchBar v-model="keyword" @search="onSearch" />
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
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';

import NnCard from '../components/common/NnCard.vue';
import ShoppingSearchBar from '../components/shopping/ShoppingSearchBar.vue';
import ShoppingProductCard from '../components/shopping/ShoppingProductCard.vue';
import { searchProducts, getRecommendations } from '../api/shoppingApi.js';
import { fetchPlanIngredients } from '../api/mealPlanApi.js';

// --- 식단 재료 불러오기
const route = useRoute();
const planId = computed(() => route.query.planId);
const ingredients = ref([]);
const isLoadingIngredients = ref(false);
const ingredientsError = ref('');

watch(
  planId,
  async (val) => {
    if (!val) {
      ingredients.value = [];
      return;
    }
    isLoadingIngredients.value = true;
    ingredientsError.value = '';
    try {
      ingredients.value = await fetchPlanIngredients(val);
    } catch (e) {
      console.error(e);
      ingredientsError.value = '식단 재료 정보를 불러오는 중 오류가 발생했습니다.';
    } finally {
      isLoadingIngredients.value = false;
    }
  },
  { immediate: true }
);


// --- 상품 검색
const keyword = ref('');
const searchResults = ref([]);
const recommendations = ref([]);
const isLoading = ref(false);
const isLoadingReco = ref(false);
const errorMessage = ref('');

async function onSearch() {
  if (!keyword.value || !keyword.value.trim()) {
    errorMessage.value = '검색어를 입력해 주세요.';
    return;
  }

  isLoading.value = true;
  isLoadingReco.value = true;
  errorMessage.value = '';
  searchResults.value = [];
  recommendations.value = [];

  try {
    const searchTerm = keyword.value.trim();
    // 1) 일반 검색
    searchResults.value = await searchProducts(searchTerm);

    // 2) 추천 (일단 neededGram은 500g 고정으로 테스트)
    recommendations.value = await getRecommendations(searchTerm, 500);
  } catch (err) {
    console.error(err);
    errorMessage.value = '검색 중 오류가 발생했습니다.';
  } finally {
    isLoading.value = false;
    isLoadingReco.value = false;
  }
}

function searchFromIngredient(name) {
  keyword.value = name;
  onSearch();
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

.ingredients-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ingredients-list__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background-color: #f9fafb;
  border-radius: 8px;
}

.ingredients-list__info {
  font-size: 14px;
  color: #374151;
}

.ingredients-list__info strong {
  font-weight: 600;
  color: #111827;
}

.ingredients-list__info span {
  font-size: 13px;
  color: #6b7280;
}
</style>
