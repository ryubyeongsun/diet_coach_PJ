<template>
  <div class="page">
    <header class="page__header">
      <h1>장보기 리스트</h1>
      <p v-if="shoppingData">
        <strong>{{ shoppingData.fromDate }} ~ {{ shoppingData.toDate }}</strong>
        기간의 필요 재료입니다.
      </p>
      <p v-else>식단 플랜에 따른 재료 목록을 확인하고 구매해 보세요.</p>
    </header>

    <div v-if="isValidPlanId">
      <div class="controls">
        <div class="period-selector">
          <button
            @click="setRange('TODAY')"
            :class="{ active: range === 'TODAY' }"
          >
            오늘
          </button>
          <button
            @click="setRange('WEEK')"
            :class="{ active: range === 'WEEK' }"
          >
            이번 주
          </button>
          <button
            @click="setRange('MONTH')"
            :class="{ active: range === 'MONTH' }"
          >
            이번 달
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="page__status">
        <p>장보기 목록을 불러오는 중입니다...</p>
      </div>
      <div v-else-if="error" class="page__error">
        <p>{{ error }}</p>
      </div>
      <div
        v-else-if="shoppingData && shoppingData.items.length > 0"
        class="item-list"
      >
        <ShoppingItemCard
          v-for="(item, index) in shoppingData.items"
          :key="index"
          :item="item"
        />
      </div>
      <div v-else class="page__status">
        <p>표시할 재료가 없습니다.</p>
      </div>
    </div>

    <div v-else class="page__error">
      <p>
        유효한 식단 정보가 없습니다. 식단 관리 페이지로 돌아가서 다시
        시도해주세요.
      </p>
      <NnButton @click="goBack">돌아가기</NnButton>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchShoppingList } from "../api/shoppingApi.js";
import { fetchLatestMealPlan } from "../api/mealPlanApi.js";
import { getCurrentUser } from "../utils/auth";
import ShoppingItemCard from "../components/shopping/ShoppingItemCard.vue";
import NnButton from "../components/common/NnButton.vue";

const route = useRoute();
const router = useRouter();

const planId = ref(null);
const isValidPlanId = ref(false);
const range = ref("MONTH"); // 기본값 MONTH
const shoppingData = ref(null);
const isLoading = ref(false);
const error = ref("");

async function loadShoppingList() {
  if (!isValidPlanId.value) return;

  isLoading.value = true;
  error.value = "";
  // shoppingData.value = null; // 이전 데이터 clear

  try {
    const response = await fetchShoppingList(planId.value, range.value);
    shoppingData.value = response;
  } catch (err) {
    console.error("Error fetching shopping list:", err);
    if (err.response?.status === 401) {
      router.push("/login");
    } else if (err.response?.status === 404) {
      error.value = "장보기 기능은 아직 준비 중입니다. 잠시만 기다려 주세요.";
    } else {
      error.value = "장보기 정보를 불러올 수 없습니다. 다시 시도해주세요.";
    }
    shoppingData.value = null;
  } finally {
    isLoading.value = false;
  }
}

function setRange(newRange) {
  range.value = newRange;
}

function goBack() {
  router.push("/meal-plans");
}

watch(range, () => {
  // range가 변경되면 데이터를 다시 불러옵니다.
  loadShoppingList();
});

onMounted(async () => {
  const id = route.query.planId;
  if (id && !isNaN(id)) {
    planId.value = Number(id);
    isValidPlanId.value = true;
    loadShoppingList(); // planId가 유효하면 데이터 로드 시작
  } else {
    // planId가 쿼리로 넘어오지 않은 경우, 사용자 최신 플랜 조회 시도
    const user = getCurrentUser();
    if (user && user.id) {
      try {
        isLoading.value = true;
        const latestPlan = await fetchLatestMealPlan(user.id);
        if (latestPlan && latestPlan.mealPlanId) {
          planId.value = latestPlan.mealPlanId;
          isValidPlanId.value = true;
          loadShoppingList();
        } else {
          isValidPlanId.value = false;
        }
      } catch (e) {
        console.error("Failed to fetch latest meal plan", e);
        isValidPlanId.value = false;
      } finally {
        isLoading.value = false;
      }
    } else {
      isValidPlanId.value = false;
    }
  }
});
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
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
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

.controls {
  margin-bottom: 20px;
}

.period-selector {
  display: inline-flex;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #d1d5db;
}

.period-selector button {
  padding: 10px 20px;
  border: none;
  background-color: #ffffff;
  color: #374151;
  cursor: pointer;
  transition:
    background-color 0.2s,
    color 0.2s;
  font-size: 14px;
  font-weight: 500;
}

.period-selector button:not(:last-child) {
  border-right: 1px solid #d1d5db;
}

.period-selector button.active {
  background-color: #3b82f6;
  color: #ffffff;
}

.period-selector button:hover:not(.active) {
  background-color: #f3f4f6;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page__status,
.page__error {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  background-color: #f9fafb;
  border-radius: 8px;
}

.page__error {
  color: #dc2626;
  background-color: #fef2f2;
}
</style>
