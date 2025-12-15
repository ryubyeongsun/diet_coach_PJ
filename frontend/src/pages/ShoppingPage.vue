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
    
    <!-- (이하 생략) -->
    
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
        // 날짜를 못불러와도 재료는 보여줄 수 있으므로 에러를 막지는 않음
      }
      
      if (ingredientsResult.status === 'fulfilled') {
        const rawIngredients = ingredientsResult.value || [];
        // 재료 집계 로직
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

// (이하 생략)

</script>

<style scoped>
/* (이하 생략) */
.plan-period {
  font-size: 13px;
  color: #4b5563;
  background-color: #f3f4f6;
  padding: 6px 10px;
  border-radius: 8px;
  display: inline-block;
  margin-bottom: 16px;
}
</style>
