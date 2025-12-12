<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>식단 재료 쇼핑</h1>
        <p>
          식단에 필요한 재료를 확인하고, 11번가 상품을 검색하여 합리적인
          소비를 도와드립니다.
        </p>
        <p v-if="planId" class="page__sub-header">
          현재 플랜 ID: {{ planId }}
        </p>
      </div>
    </header>

    <!-- 1. 식단 재료 목록 -->
    <NnCard v-if="planId" title="이 식단에 필요한 재료">
      <div v-if="isLoadingIngredients" class="page__status">
        재료 정보를 불러오는 중입니다...
      </div>
      <div v-else-if="ingredientsError" class="page__error">
        {{ ingredientsError }}
      </div>
      <div v-else-if="!ingredients || ingredients.length === 0" class="page__status">
        이 식단에서 집계된 재료가 없습니다.
      </div>
      <ul v-else class="ingredients-list">
        <li
          v-for="ing in ingredients"
          :key="ing.ingredientName"
          class="ingredients-list__item"
        >
          <div class="ingredients-list__info">
            <strong>{{ ing.ingredientName }}</strong>
            <span v-if="ing.totalGram">총 {{ ing.totalGram }}g 필요</span>
          </div>
          <NnButton
            size="sm"
            variant="outline"
            @click="searchFromIngredient(ing.ingredientName)"
          >
            이 재료 상품 찾기
          </NnButton>
        </li>
      </ul>
    </NnCard>

    <NnCard v-else title="안내">
      <p class="page__status">
        먼저 식단 페이지에서 "이 식단 재료 장보기"를 눌러주세요.
      </p>
    </NnCard>

    <!-- 2. 재료 검색 -->
    <NnCard title="재료 검색">
      <ShoppingSearchBar v-model="keyword" @search="onSearch" />
      <p v-if="searchError" class="page__error">
        {{ searchError }}
      </p>
    </NnCard>

    <!-- 3. 검색 결과 -->
    <NnCard :title="`'${keyword}' 검색 결과`" v-if="hasSearched">
      <div v-if="isLoadingSearch" class="page__status">
        검색 중입니다...
      </div>
      <div v-else-if="searchResults.length === 0" class="page__status">
        검색 결과가 없습니다.
      </div>
      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in searchResults"
          :key="p.externalId"
          :product="p"
          @add-to-cart="addToCart"
        />
      </div>
    </NnCard>

    <!-- 4. 추천 상품 -->
    <NnCard :title="`'${keyword}' 추천 상품 (500g 기준)`" v-if="hasSearched">
      <div v-if="isLoadingReco" class="page__status">
        추천 상품을 불러오는 중입니다...
      </div>
       <p v-if="recoError" class="page__error">
        {{ recoError }}
      </p>
      <div v-else-if="recommendations.length === 0" class="page__status">
        추천 상품이 없습니다.
      </div>
      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in recommendations"
          :key="`reco-${p.externalId}`"
          :product="p"
          @add-to-cart="addToCart"
        />
      </div>
    </NnCard>

    <!-- 5. 임시 장바구니 요약 -->
    <NnCard title="선택한 상품 요약">
      <div v-if="cartSummary.totalCount === 0" class="page__status">
        담은 상품이 없습니다.
      </div>
      <div v-else class="cart-summary">
        <div class="summary-line">
          <span>담은 상품:</span>
          <strong>{{ cartSummary.totalCount }}개</strong>
        </div>
        <div class="summary-line">
          <span>총 금액:</span>
          <strong>{{ cartSummary.totalPrice.toLocaleString() }}원</strong>
        </div>
        <div v-if="cartSummary.totalGram > 0" class="summary-line">
          <span>총 무게:</span>
          <strong>약 {{ cartSummary.totalGram.toLocaleString() }}g</strong>
        </div>
        <div class="cart-summary__actions">
          <NnButton size="sm" @click="clearCart">
            선택 초기화
          </NnButton>
        </div>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';

import NnCard from '../components/common/NnCard.vue';
import NnButton from '../components/common/NnButton.vue';
import ShoppingSearchBar from '../components/shopping/ShoppingSearchBar.vue';
import ShoppingProductCard from '../components/shopping/ShoppingProductCard.vue';
import { searchProducts, getRecommendations } from '../api/shoppingApi.js';
import { fetchPlanIngredients } from '../api/mealPlanApi.js';

// --- 식단 재료 ---
const route = useRoute();
const planId = computed(() =>
  route.query.planId ? Number(route.query.planId) : null
);
const ingredients = ref([]);
const isLoadingIngredients = ref(false);
const ingredientsError = ref('');

watch(
  planId,
  async (newPlanId) => {
    if (!newPlanId) {
      ingredients.value = [];
      return;
    }
    isLoadingIngredients.value = true;
    ingredientsError.value = '';
    try {
      const response = await fetchPlanIngredients(newPlanId);
      ingredients.value = response || [];
    } catch (e) {
      console.error('fetchPlanIngredients error:', e);
      ingredientsError.value = '식단 재료를 불러오는 중 오류가 발생했습니다.';
    } finally {
      isLoadingIngredients.value = false;
    }
  },
  { immediate: true }
);

// --- 상품 검색 및 추천 ---
const keyword = ref('');
const searchResults = ref([]);
const recommendations = ref([]);
const isLoadingSearch = ref(false);
const isLoadingReco = ref(false);
const searchError = ref('');
const recoError = ref('');
const hasSearched = ref(false);

async function executeSearch() {
  if (!keyword.value || !keyword.value.trim()) {
    searchError.value = '검색어를 입력해 주세요.';
    return;
  }
  
  hasSearched.value = true;

  isLoadingSearch.value = true;
  isLoadingReco.value = true;
  searchError.value = '';
  recoError.value = '';
  searchResults.value = [];
  recommendations.value = [];

  const searchTerm = keyword.value.trim();

  const [searchResult, recoResult] = await Promise.allSettled([
    searchProducts(searchTerm),
    getRecommendations(searchTerm, 500),
  ]);

  if (searchResult.status === 'fulfilled') {
    searchResults.value = searchResult.value || [];
  }
  else {
    console.error('searchProducts error:', searchResult.reason);
    searchError.value = '상품 검색 중 오류가 발생했습니다.';
  }
  isLoadingSearch.value = false;

  if (recoResult.status === 'fulfilled') {
    recommendations.value = recoResult.value || [];
  }
  else {
    console.error('getRecommendations error:', recoResult.reason);
    recoError.value = '추천 상품을 불러오는 중 오류가 발생했습니다.';
  }
  isLoadingReco.value = false;
}

function onSearch() {
  executeSearch();
}

function searchFromIngredient(name) {
  keyword.value = name;
  executeSearch();
}

// --- 임시 장바구니 ---
const selectedProducts = ref([]);

const cartSummary = computed(() => {
  const totalCount = selectedProducts.value.length;
  
  const totalPrice = selectedProducts.value.reduce((sum, product) => {
    return sum + (product.price || 0);
  }, 0);

  const totalGram = selectedProducts.value.reduce((sum, product) => {
    return sum + (product.gramPerUnit || 0);
  }, 0);

  return {
    totalCount,
    totalPrice,
    totalGram,
  };
});

function addToCart(product) {
  selectedProducts.value.push(product);
}

function clearCart() {
  selectedProducts.value = [];
}
</script>

<style scoped>
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.page__sub-header {
  margin-top: 8px !important;
  font-size: 12px;
  font-weight: 500;
  color: #4b5563;
  background-color: #f3f4f6;
  padding: 4px 8px;
  border-radius: 6px;
  display: inline-block;
}

.page__error {
  margin-top: 8px;
  font-size: 13px;
  color: #dc2626;
}

.page__status {
  padding: 16px 0;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.page__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  padding: 12px;
  background-color: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  transition: background-color 0.2s;
}
.ingredients-list__item:hover {
  background-color: #f3f4f6;
}

.ingredients-list__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 14px;
  color: #374151;
}

.ingredients-list__info strong {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.ingredients-list__info span {
  font-size: 13px;
  color: #4b5563;
}

.cart-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.summary-line strong {
  font-weight: 700;
  font-size: 16px;
}
.cart-summary__actions {
  margin-top: 12px;
  border-top: 1px solid #f3f4f6;
  padding-top: 12px;
  text-align: right;
}
</style>
