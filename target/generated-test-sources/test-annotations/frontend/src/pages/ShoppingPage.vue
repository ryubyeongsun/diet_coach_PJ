<script setup>
import { ref } from 'vue';
import axios from 'axios';

// 상대경로 import (@ 안 씀)
import NnCard from '../components/common/NnCard.vue';
import ShoppingSearchBar from '../components/shopping/ShoppingSearchBar.vue';
import ShoppingProductCard from '../components/shopping/ShoppingProductCard.vue';

/* ----------------- 검색 상태 ----------------- */
const products = ref([]);
const loading = ref(false);
const errorMessage = ref('');

/* ----------------- 추천 상태 ----------------- */
const recommendIngredient = ref('닭가슴살'); // 기본값 예시
const recommendGram = ref(500);
const recommendProducts = ref([]);
const recommendLoading = ref(false);
const recommendError = ref('');

/* ========== 검색 핸들러 ========== */
const onSearch = async (keyword) => {
  const trimmed = keyword.trim();
  if (!trimmed) {
    errorMessage.value = '검색어를 입력해주세요.';
    products.value = [];
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  products.value = [];

  try {
    const res = await axios.get(
      'http://localhost:8080/api/shopping/search',
      {
        params: { keyword: trimmed },
      }
    );

    if (res.data && res.data.success) {
      products.value = res.data.data ?? [];
      if (products.value.length === 0) {
        errorMessage.value = '검색 결과가 없습니다.';
      }
    } else {
      errorMessage.value =
        res.data?.message || '검색 중 오류가 발생했습니다.';
    }
  } catch (e) {
    console.error('[ShoppingPage] 검색 중 오류', e);
    errorMessage.value = '검색 중 오류가 발생했습니다.';
  } finally {
    loading.value = false;
  }
};

/* ========== 추천 핸들러 ========== */
const onRecommend = async () => {
  const ingredient = recommendIngredient.value.trim();
  const gram = Number(recommendGram.value);

  if (!ingredient) {
    recommendError.value = '재료명을 입력해주세요.';
    recommendProducts.value = [];
    return;
  }
  if (!gram || gram <= 0) {
    recommendError.value = '필요량(g)을 올바르게 입력해주세요.';
    recommendProducts.value = [];
    return;
  }

  recommendLoading.value = true;
  recommendError.value = '';
  recommendProducts.value = [];

  try {
    const res = await axios.get(
      'http://localhost:8080/api/shopping/recommendations',
      {
        params: {
          ingredient,
          neededGram: gram,
        },
      }
    );

    if (res.data && res.data.success) {
      recommendProducts.value = res.data.data ?? [];
      if (recommendProducts.value.length === 0) {
        recommendError.value = '추천 결과가 없습니다.';
      }
    } else {
      recommendError.value =
        res.data?.message || '추천 계산 중 오류가 발생했습니다.';
    }
  } catch (e) {
    console.error('[ShoppingPage] 추천 호출 오류', e);
    recommendError.value = '추천 계산 중 오류가 발생했습니다.';
  } finally {
    recommendLoading.value = false;
  }
};
</script>


<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>식단 재료 쇼핑 추천</h1>
        <p>
          11번가 API와 연동해, 식단에 필요한 재료를 가격 비교와 함께 추천해주는 화면입니다.
          지금은 키워드 검색과 필요량 기준 추천까지 연동한 상태입니다.
        </p>
      </div>
    </header>

    <!-- ① 키워드 검색 -->
    <NnCard title="재료 검색">
      <ShoppingSearchBar @search="onSearch" />
    </NnCard>

    <NnCard title="검색 결과">
      <div v-if="loading" class="page__empty">
        검색 중입니다…
      </div>

      <div v-else-if="errorMessage" class="page__empty page__empty--error">
        {{ errorMessage }}
      </div>

      <div v-else-if="products.length === 0" class="page__empty">
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

    <!-- ② 필요량 기준 추천 폼 -->
    <NnCard title="필요량 기준 추천 (예: 식단 생성 시 사용)">
      <div class="recommend-form">
        <div class="recommend-form__field">
          <label>재료명</label>
          <input
            v-model="recommendIngredient"
            type="text"
            placeholder="예) 닭가슴살"
          />
        </div>
        <div class="recommend-form__field">
          <label>필요량 (g)</label>
          <input
            v-model="recommendGram"
            type="number"
            min="1"
            step="50"
          />
        </div>
        <button class="recommend-form__button" @click="onRecommend">
          추천 받기
        </button>
      </div>
    </NnCard>

    <!-- ③ 추천 결과 -->
    <NnCard title="추천 결과">
      <div v-if="recommendLoading" class="page__empty">
        추천을 계산하는 중입니다…
      </div>

      <div
        v-else-if="recommendError"
        class="page__empty page__empty--error"
      >
        {{ recommendError }}
      </div>

      <div
        v-else-if="recommendProducts.length === 0"
        class="page__empty"
      >
        재료명과 필요량(g)을 입력하고 ‘추천 받기’를 눌러보세요.
      </div>

      <div v-else class="page__list">
        <ShoppingProductCard
          v-for="p in recommendProducts"
          :key="p.externalId"
          :product="p"
        />
      </div>
    </NnCard>
  </div>
</template>


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

.page__empty {
  font-size: 13px;
  color: #6b7280;
}

.page__empty--error {
  color: #ef4444;
}

.page__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 추천 폼 레이아웃 */
.recommend-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.recommend-form__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recommend-form__field label {
  font-size: 12px;
  color: #4b5563;
}

.recommend-form__field input {
  min-width: 160px;
  padding: 8px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  outline: none;
  font-size: 13px;
}

.recommend-form__field input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.25);
}

.recommend-form__button {
  padding: 10px 18px;
  border-radius: 999px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  background: #4f46e5;
  color: #ffffff;
  cursor: pointer;
  white-space: nowrap;
}

.recommend-form__button:hover {
  background: #4338ca;
}
</style>
