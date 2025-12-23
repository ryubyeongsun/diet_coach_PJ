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

      <div v-else-if="shoppingData && shoppingData.items.length > 0" class="shopping-layout">
        
        <!-- 왼쪽: 메인 리스트 영역 -->
        <div class="shopping-main">
          <!-- 요약 카드 -->
          <div class="summary-bar">
            <div class="summary-info">
              <span class="label">총 예상 비용 (전체)</span>
              <span class="value">{{ totalPrice.toLocaleString() }}원</span>
            </div>
          </div>

          <!-- 리스트 -->
          <TransitionGroup name="list" tag="div" class="item-list">
            <ShoppingItemCard
              v-for="item in sortedShoppingItems"
              :key="getItemKey(item)"
              :item="item"
              :is-checked="isItemInCart(item)"
              :is-purchased="globalState.confirmed.has(getItemKey(item))"
              @toggle="toggleCheck(item)"
            />
          </TransitionGroup>
        </div>

        <!-- 오른쪽: 영수증 사이드바 -->
        <div class="shopping-sidebar">
          <div class="receipt-card">
            <div class="receipt-header">
              <h3>장바구니 영수증</h3>
              <span class="count">{{ globalState.cart.length }}</span>
            </div>
            
            <div class="receipt-body">
              <div v-if="globalState.cart.length === 0" class="empty-receipt">
                목록에서 재료를 선택하여<br>예산 계획을 세워보세요.
              </div>
              <TransitionGroup v-else name="list" tag="ul" class="selected-items">
                <li 
                  v-for="item in sortedSelectedItems" 
                  :key="item.productCode"
                  :class="{ 'is-purchased': purchasedIndices.has(item.productCode) }"
                >
                  <input 
                    type="checkbox" 
                    class="receipt-check"
                    :checked="purchasedIndices.has(item.productCode)"
                    @change="togglePurchased(item.productCode)"
                  />
                  <div class="item-details">
                    <div class="ingredient-row">
                      <span class="ingredient-name">{{ item.ingredientName }}</span>
                      <span class="price">{{ item.price.toLocaleString() }}원</span>
                    </div>
                    <a 
                      v-if="item.productUrl" 
                      :href="item.productUrl" 
                      target="_blank" 
                      class="name-link"
                    >
                      {{ item.name }} 🔗
                    </a>
                    <span v-else class="name">{{ item.name }}</span>
                  </div>
                </li>
              </TransitionGroup>
            </div>

            <div class="receipt-footer">
              <div class="total-row">
                <span>예상 합계</span>
                <span class="total-price">{{ selectedTotalPrice.toLocaleString() }}원</span>
              </div>
              
              <div class="footer-buttons">
                <button 
                  class="bulk-add-btn" 
                  @click="confirmPurchase" 
                  :disabled="globalState.cart.length === 0"
                >
                  ✅ 구매 확정
                </button>
                <button class="go-cart-btn" @click="goToLedgerPage">
                  💰 식단 가계부 확인
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>

      <div v-else class="page__status">
        <p>구매할 재료가 없습니다.</p>
        <button v-if="range !== 'MONTH'" class="retry-btn" @click="setRange('MONTH')">
          전체 기간으로 보기
        </button>
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
import { globalState, addToCart, removeFromCart } from "../utils/globalState";
import ShoppingItemCard from "../components/shopping/ShoppingItemCard.vue";
import NnButton from "../components/common/NnButton.vue";

const route = useRoute();
const router = useRouter();

const planId = ref(null);
const isValidPlanId = ref(false);
const range = ref("MONTH");
const shoppingData = ref(null);
const isLoading = ref(false);
const error = ref("");

// 영수증 내 체크 표시 (취소선용)
const purchasedIndices = ref(new Set());

// Helper to get a unique key for an item (Product ID > Ingredient Name)
const getItemKey = (item) => {
  if (item.product && item.product.externalId) {
    return item.product.externalId;
  }
  return 'ing_' + item.ingredientName;
};

// Sync check with global cart
const isItemInCart = (item) => {
  const key = getItemKey(item);
  return globalState.cart.some(c => c.productCode === key);
};

// Total price of ALL items in the list
const totalPrice = computed(() => {
  if (!shoppingData.value) return 0;
  return shoppingData.value.items.reduce((sum, item) => {
    if (item.product && item.product.price) {
      return sum + item.product.price;
    }
    return sum;
  }, 0);
});

// 정렬된 장보기 리스트 (체크된 항목 상단, 구매 완료 항목 최하단)
const sortedShoppingItems = computed(() => {
  if (!shoppingData.value || !shoppingData.value.items) return [];
  return [...shoppingData.value.items].sort((a, b) => {
    const aKey = getItemKey(a);
    const bKey = getItemKey(b);
    const aPurchased = globalState.confirmed.has(aKey);
    const bPurchased = globalState.confirmed.has(bKey);
    
    // 1. 구매 완료된 항목은 무조건 뒤로
    if (aPurchased !== bPurchased) return aPurchased ? 1 : -1;
    
    // 2. 장바구니에 담긴(체크된) 항목은 위로
    const aInCart = isItemInCart(a);
    const bInCart = isItemInCart(b);
    if (aInCart !== bInCart) return aInCart ? -1 : 1;
    
    return 0;
  });
});

// Selected items list (from Global Cart)
const selectedTotalPrice = computed(() => {
  return globalState.cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
});

// 정렬된 영수증 리스트 (장바구니 항목 중 아직 확정되지 않은 것)
const sortedSelectedItems = computed(() => {
  return globalState.cart
    .filter(item => !globalState.confirmed.has(item.productCode))
    .map(item => ({
      productCode: item.productCode,
      ingredientName: item.ingredientName || item.name,
      name: item.name,
      price: item.price,
      productUrl: item.productUrl
    }));
});

// 리스트에서 체크/해제 시 장바구니 동기화
function toggleCheck(item) {
  const key = getItemKey(item);
  
  if (isItemInCart(item)) {
    removeFromCart(key);
  } else {
    // Construct product object for cart
    const productToAdd = item.product ? {
      externalId: item.product.externalId || key,
      name: item.product.productName,
      ingredientName: item.ingredientName, // Important: Pass ingredient name
      price: item.product.price,
      imageUrl: item.product.imageUrl,
      productUrl: item.product.productUrl
    } : {
      externalId: key,
      name: item.ingredientName, // Product name fallback
      ingredientName: item.ingredientName, // Ingredient name
      price: 0, 
      imageUrl: '', 
      productUrl: ''
    };
    
    addToCart(productToAdd);
  }
}

// 영수증 내 취소선 토글
function togglePurchased(productCode) {
  if (purchasedIndices.value.has(productCode)) {
    purchasedIndices.value.delete(productCode);
  } else {
    purchasedIndices.value.add(productCode);
  }
}

function goToLedgerPage() {
  router.push('/cart'); // /cart is the Ledger page
}

// 구매 확정 알림 및 이동
function confirmPurchase() {
  if (purchasedIndices.value.size === 0) {
    alert("구매를 확정할 항목을 선택해주세요.");
    return;
  }

  // 체크된 항목들을 confirmed 세트에 추가 및 상세 정보 저장
  purchasedIndices.value.forEach(code => {
    const itemInCart = globalState.cart.find(item => item.productCode === code);
    if (itemInCart) {
      // 가계부 기록을 위해 전체 객체 저장
      globalState.purchasedItems.push({
        ...itemInCart,
        purchasedAt: new Date().toISOString()
      });
    }
    
    globalState.confirmed.add(code);
    removeFromCart(code); // 장바구니에서도 제거
  });

  alert(`${purchasedIndices.value.size}개의 상품 구매가 확정되었습니다.`);
  purchasedIndices.value.clear(); // 영수증 체크 초기화
}

async function loadShoppingList() {
  if (!isValidPlanId.value) return;

  isLoading.value = true;
  error.value = "";

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
  max-width: 1200px;
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

/* --- Layout --- */
.shopping-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.shopping-main {
  flex: 1;
  min-width: 0;
}

.shopping-sidebar {
  width: 320px;
  position: sticky;
  top: 24px;
  flex-shrink: 0;
}

/* --- Main Area --- */
.summary-bar {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

/* Item List */
.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* --- Sidebar Receipt --- */
.receipt-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  min-height: 400px;
}

.receipt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 2px dashed #e5e7eb;
  margin-bottom: 16px;
}

.receipt-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.receipt-header .count {
  background-color: #ecfdf5;
  color: #047857;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.receipt-body {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
  margin-bottom: 16px;
}

.empty-receipt {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 40px 0;
  line-height: 1.5;
}

.selected-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
}

.selected-items li {
  display: flex;
  align-items: flex-start;
  font-size: 13px;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  gap: 8px;
}

.selected-items li:last-child {
  border-bottom: none;
}

.receipt-check {
  margin-top: 2px;
  cursor: pointer;
}

.item-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ingredient-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ingredient-name {
  font-weight: 700;
  color: #111827;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  padding-right: 8px;
}

.selected-items li .price {
  font-weight: 600;
  color: #6b7280;
  font-size: 13px;
  flex-shrink: 0;
}

.name-link {
  color: #059669;
  font-size: 12px;
  text-decoration: none;
  display: block;
}

.name-link:hover {
  text-decoration: underline;
}

/* Strikethrough style for purchased items */
.selected-items li.is-purchased .item-details {
  opacity: 0.5;
  text-decoration: line-through;
}

.receipt-footer {
  border-top: 2px solid #e5e7eb;
  padding-top: 16px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.total-row span:first-child {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.total-price {
  font-size: 22px;
  font-weight: 800;
  color: #059669;
}

.footer-buttons {
  display: flex;
  gap: 8px;
}

.bulk-add-btn {
  flex: 2;
  padding: 14px;
  background-color: #111827;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.go-cart-btn {
  flex: 1;
  padding: 14px;
  background-color: white;
  color: #374151;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.go-cart-btn:hover {
  background-color: #f9fafb;
}

.bulk-add-btn:hover:not(:disabled) {
  background-color: #374151;
  transform: translateY(-2px);
}

.bulk-add-btn:disabled {
  background-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
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

/* --- List Transition --- */
.list-move {
  transition: transform 0.5s ease;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 900px) {
  .shopping-layout {
    flex-direction: column;
  }
  .shopping-sidebar {
    width: 100%;
    position: static;
  }
  .receipt-body {
    max-height: 200px;
  }
}
</style>
