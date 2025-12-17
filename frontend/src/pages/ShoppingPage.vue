<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>식단 재료 쇼핑</h1>
        <p>
          식단에 필요한 재료를 확인하고, 11번가 상품을 검색하여 합리적인
          소비를 도와드립니다.
        </p>
      </div>
    </header>

    <!-- 1. 식단 재료 목록 -->
    <NnCard v-if="planId" title="이번 달 식단 재료 장보기">
      <div v-if="planDetails" class="plan-period">
        {{ planDetails.startDate }} ~ {{ planDetails.endDate }}
      </div>

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
            <span v-if="ing.totalGram">총 {{ ing.totalGram.toLocaleString() }}g 필요</span>
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
        먼저 식단 페이지에서 "이번 달 식단 재료 장보기"를 눌러주세요.
      </p>
    </NnCard>

    <!-- 2. 상품 직접 검색 -->
    <NnCard title="상품 직접 검색">
      <ShoppingSearchBar @search="handleSearch" :initial-query="searchQuery" />
    </NnCard>

    <!-- 3. 검색 결과 -->
    <NnCard title="상품 검색 결과" v-if="searchedProducts !== null">
      <div v-if="isLoadingSearch" class="page__status">상품을 검색 중입니다...</div>
      <div v-else-if="searchError" class="page__error">{{ searchError }}</div>
      <div v-else-if="searchedProducts.length === 0" class="page__status">검색 결과가 없습니다.</div>
      <div v-else class="products-grid">
        <ShoppingProductCard
          v-for="p in searchedProducts"
          :key="p.externalId"
          :product="p"
          @add-to-cart="handleAddToCart"
        />
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
import { fetchPlanIngredients, fetchMealPlan } from '../api/mealPlanApi.js';
import { addToCart } from '../utils/globalState.js';

// --- 식단 재료 ---
const route = useRoute();
const planId = computed(() =>
  route.query.planId ? Number(route.query.planId) : null
);
const ingredients = ref([]);
const planDetails = ref(null);
const isLoadingIngredients = ref(false);
const ingredientsError = ref('');

watch(
  planId,
  async (newPlanId) => {
    if (!newPlanId) {
      ingredients.value = [];
      planDetails.value = null;
      return;
    }
    isLoadingIngredients.value = true;
    ingredientsError.value = '';
    try {
      const [ingredientsResult, planResult] = await Promise.allSettled([
        fetchPlanIngredients(newPlanId),
        fetchMealPlan(newPlanId),
      ]);

      if (planResult.status === 'fulfilled') {
        planDetails.value = planResult.value;
      }
      else {
        console.error('fetchMealPlan error:', planResult.reason);
      }
      
      if (ingredientsResult.status === 'fulfilled') {
        const rawIngredients = ingredientsResult.value || [];
        const ingredientMap = new Map();
        rawIngredients.forEach(ing => {
          if (ingredientMap.has(ing.ingredientName)) {
            const existing = ingredientMap.get(ing.ingredientName);
            existing.totalGram += ing.totalGram;
          }
          else {
            ingredientMap.set(ing.ingredientName, { ...ing });
          }
        });
        ingredients.value = Array.from(ingredientMap.values());
      }
      else {
        throw ingredientsResult.reason;
      }

    } catch (e) {
      console.error('재료 로딩 프로세스 에러:', e);
      ingredientsError.value = '식단 재료를 불러오는 중 오류가 발생했습니다.';
    } finally {
      isLoadingIngredients.value = false;
    }
  },
  { immediate: true }
);


// --- 상품 검색 ---
const searchQuery = ref('');
const searchedProducts = ref(null);
const isLoadingSearch = ref(false);
const searchError = ref('');

async function executeSearch(keyword) {
  if (isLoadingSearch.value) return; // 중복 호출 방지 가드
  if (!keyword?.trim()) return;
  
  searchQuery.value = keyword;
  isLoadingSearch.value = true;
  searchError.value = '';
  searchedProducts.value = null;

  try {
    const results = await searchProducts(keyword);
    searchedProducts.value = results.products;
  } catch (e) {
    console.error('상품 검색 에러:', e);
    searchError.value = '상품을 검색하는 중 오류가 발생했습니다.';
    searchedProducts.value = []; // 에러 발생 시에도 카드 표시를 위해 빈 배열 할당
  } finally {
    isLoadingSearch.value = false;
  }
}

function searchFromIngredient(ingredientName) {
  executeSearch(ingredientName);
  // Scroll to search results for better UX
  // This requires the result card to be rendered, so we do it in the next tick.
  setTimeout(() => {
    const resultsCard = document.querySelector('[title="상품 검색 결과"]');
    if (resultsCard) {
      resultsCard.scrollIntoView({ behavior: 'smooth' });
    }
  }, 100);
}

function handleSearch(keyword) {
  executeSearch(keyword);
}

function handleAddToCart(product) {
  addToCart(product);
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
.page__header {
  margin-bottom: 8px;
}
.page__header h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}
.page__header p {
  font-size: 14px;
  color: #6b7280;
}
.page__status {
  padding: 20px;
  text-align: center;
  color: #4b5563;
  font-size: 14px;
}
.page__error {
  padding: 20px;
  text-align: center;
  color: #ef4444;
  font-size: 14px;
  background-color: #fef2f2;
  border-radius: 8px;
}

.plan-period {
  font-size: 13px;
  color: #4b5563;
  background-color: #f3f4f6;
  padding: 6px 10px;
  border-radius: 8px;
  display: inline-block;
  margin-bottom: 16px;
}
.ingredients-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.ingredients-list__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: #f9fafb;
  border-radius: 8px;
}
.ingredients-list__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ingredients-list__info strong {
  font-weight: 600;
}
.ingredients-list__info span {
  font-size: 12px;
  color: #6b7280;
}
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
</style>
