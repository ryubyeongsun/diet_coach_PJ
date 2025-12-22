<template>
  <div class="page">
    <header class="page__header">
      <div class="header-top">
        <h1>장보기 리스트</h1>
        <div class="period-selector">
          <button @click="setRange('TODAY')" :class="{ active: range === 'TODAY' }">오늘</button>
          <button @click="setRange('WEEK')" :class="{ active: range === 'WEEK' }">이번 주</button>
          <button @click="setRange('MONTH')" :class="{ active: range === 'MONTH' }">이번 달</button>
        </div>
      </div>
      <p v-if="shoppingData && shoppingData.fromDate && shoppingData.toDate" class="date-range">
        {{ shoppingData.fromDate }} ~ {{ shoppingData.toDate }}
      </p>
    </header>

    <div v-if="isValidPlanId" class="content">
      <div v-if="isLoading" class="page__status">
        <div class="spinner"></div>
        <p>재료 정보를 분석하고 있어요...</p>
      </div>
      
      <div v-else-if="error" class="page__error">
        <p>{{ error }}</p>
      </div>

      <div v-else-if="shoppingData && shoppingData.items.length > 0">
        <!-- 요약 및 일괄 작업 카드 -->
        <div class="summary-bar">
          <div class="summary-info">
            <span class="label">총 예상 비용</span>
            <span class="value">{{ totalPrice.toLocaleString() }}원</span>
          </div>
          <div class="summary-actions">
            <button class="bulk-btn" @click="addSelectedToCart" :disabled="checkedItems.size === 0">
              선택 담기 ({{ checkedItems.size }})
            </button>
          </div>
        </div>

        <!-- 그리드 리스트 -->
        <div class="item-grid">
          <ShoppingItemCard
            v-for="(item, index) in shoppingData.items"
            :key="index"
            :item="item"
            :is-checked="checkedItems.has(index)"
            @toggle="toggleCheck(index)"
            @add-to-cart="handleAddSingle"
          />
        </div>
      </div>

      <div v-else class="page__status">
        <p>구매할 재료가 없습니다.</p>
      </div>
    </div>

    <div v-else class="page__error">
      <p>식단 플랜이 없습니다.</p>
      <NnButton @click="goBack">식단 생성하러 가기</NnButton>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchShoppingList } from "../api/shoppingApi.js";
import { fetchLatestMealPlan } from "../api/mealPlanApi.js";
import { getCurrentUser } from "../utils/auth";
import { addToCart } from "../utils/globalState"; // Import global cart action
import ShoppingItemCard from "../components/shopping/ShoppingItemCard.vue";
import NnButton from "../components/common/NnButton.vue";

const route = useRoute();
const router = useRouter();

const planId = ref(null);
const isValidPlanId = ref(false);
const range = ref("WEEK");
const shoppingData = ref(null);
const isLoading = ref(false);
const error = ref("");

const checkedItems = ref(new Set());

const totalPrice = computed(() => {
  if (!shoppingData.value) return 0;
  return shoppingData.value.items.reduce((sum, item) => {
    if (item.product && item.product.price) {
      return sum + item.product.price;
    }
    return sum;
  }, 0);
});

function toggleCheck(index) {
  if (checkedItems.value.has(index)) {
    checkedItems.value.delete(index);
  } else {
    checkedItems.value.add(index);
  }
}

// 개별 담기
function handleAddSingle(product) {
  addToCart(product);
}

// 일괄 담기
function addSelectedToCart() {
  if (checkedItems.value.size === 0) return;
  
  let addedCount = 0;
  checkedItems.value.forEach((index) => {
    const item = shoppingData.value.items[index];
    if (item && item.product) {
      // addToCart는 내부적으로 중복 체크 등을 수행함 (globalState 확인)
      // 하지만 alert가 매번 뜨면 귀찮으므로, globalState의 addToCart를 수정하거나
      // 여기서는 직접 push하고 한 번만 알림을 띄우는 게 나음.
      // 일단 기존 addToCart 재사용 (알림이 여러번 뜰 수 있음 - UX 개선 포인트)
      // 개선: globalState에 addMultipleToCart가 없으므로 반복 호출
      addToCart(item.product); 
      addedCount++;
    }
  });
  
  // 선택 해제
  checkedItems.value.clear();
}

async function loadShoppingList() {
  if (!isValidPlanId.value) return;

  isLoading.value = true;
  error.value = "";
  checkedItems.value.clear();

  try {
    const response = await fetchShoppingList(planId.value, range.value);
    shoppingData.value = response;
  } catch (err) {
    console.error("Error fetching shopping list:", err);
    if (err.response?.status === 401) {
      router.push("/login");
    } else if (err.response?.status === 404) {
      error.value = "아직 식단이 생성되지 않았습니다.";
    } else {
      error.value = "장보기 정보를 불러올 수 없습니다.";
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
  loadShoppingList();
});

onMounted(async () => {
  const id = route.query.planId;
  if (id && !isNaN(id)) {
    planId.value = Number(id);
    isValidPlanId.value = true;
    loadShoppingList();
  } else {
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
  max-width: 1000px; /* 넓은 그리드를 위해 폭 확장 */
  margin: 0 auto;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page__header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page__header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #111827;
}

.date-range {
  font-size: 14px;
  color: #6b7280;
  background-color: #f3f4f6;
  padding: 6px 12px;
  border-radius: 6px;
  align-self: flex-start;
}

.period-selector {
  display: flex;
  background-color: #f3f4f6;
  padding: 4px;
  border-radius: 12px;
}

.period-selector button {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
}

.period-selector button.active {
  background-color: #fff;
  color: #047857;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* 요약 바 (Sticky) */
.summary-bar {
  position: sticky;
  top: 10px; /* 헤더 아래 고정 */
  z-index: 100;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 8px;
}

.summary-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-info .label {
  font-size: 14px;
  color: #4b5563;
}

.summary-info .value {
  font-size: 20px;
  font-weight: 800;
  color: #047857;
}

.bulk-btn {
  padding: 10px 20px;
  background-color: #111827;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.bulk-btn:disabled {
  background-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.bulk-btn:not(:disabled):hover {
  background-color: #374151;
}

/* 그리드 레이아웃 */
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.page__status,
.page__error {
  padding: 60px 20px;
  text-align: center;
  color: #6b7280;
  background-color: #f9fafb;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #e5e7eb;
  border-top-color: #047857;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 600px) {
  .header-top {
    flex-direction: column;
    align-items: flex-start;
  }
  .period-selector {
    width: 100%;
    justify-content: space-between;
  }
  .period-selector button {
    flex: 1;
  }
  .summary-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  .summary-info {
    justify-content: space-between;
  }
}
</style>
