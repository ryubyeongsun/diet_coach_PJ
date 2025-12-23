<template>
  <div class="page">
    <header class="page__header">
      <h1>💰 식단 가계부</h1>
      <p>이번 달 식비 예산과 지출 계획을 스마트하게 관리하세요.</p>
    </header>

    <div class="ledger-layout">
      <!-- 1. 예산 현황 카드 -->
      <section class="budget-card">
        <div class="budget-header">
          <h2>이달의 예산 현황</h2>
          <button class="edit-btn" @click="toggleBudgetEdit">
            {{ isEditingBudget ? '저장' : '예산 설정' }}
          </button>
        </div>

        <div class="budget-display" v-if="!isEditingBudget">
          <div class="budget-row">
            <span class="label">목표 예산</span>
            <span class="value">{{ monthlyBudget.toLocaleString() }}원</span>
          </div>
          <div class="budget-row expense-actual">
            <span class="label">실제 지출 (구매 완료)</span>
            <span class="value">- {{ totalPurchasedAmount.toLocaleString() }}원</span>
          </div>
          <div class="budget-row expense">
            <span class="label">지출 예정 (담은 재료)</span>
            <span class="value">- {{ totalCartAmount.toLocaleString() }}원</span>
          </div>
          <div class="divider"></div>
          <div class="budget-row remaining">
            <span class="label">남은 예산</span>
            <span class="value" :class="{ 'warning': remainingBudget < 0 }">
              {{ remainingBudget.toLocaleString() }}원
            </span>
          </div>
        </div>

        <div class="budget-edit" v-else>
          <label>한 달 목표 식비</label>
          <div class="input-wrapper">
            <input type="number" v-model.number="monthlyBudget" step="10000" />
            <span>원</span>
          </div>
        </div>

        <!-- 그래프 -->
        <div class="progress-container">
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: usagePercentage + '%' }"
              :class="{ 'over-budget': usagePercentage > 100 }"
            ></div>
          </div>
          <div class="progress-labels">
            <span>0%</span>
            <span>{{ usagePercentage.toFixed(0) }}% 사용</span>
            <span>{{ monthlyBudget.toLocaleString() }}원</span>
          </div>
        </div>
      </section>

      <!-- 2. 지출 예정 목록 -->
      <section class="expense-list-card">
        <h2>담은 재료 목록 ({{ cartItems.length }}개)</h2>
        
        <div v-if="cartItems.length === 0" class="empty-state">
          <p>아직 담은 재료가 없습니다.</p>
          <NnButton @click="$router.push('/shopping')" variant="secondary">재료 담으러 가기</NnButton>
        </div>

        <ul v-else class="item-list">
          <li v-for="item in cartItems" :key="item.productCode" class="item-row">
            <div class="item-left">
              <span class="item-name">{{ item.name }}</span>
              <a v-if="item.productUrl" :href="item.productUrl" target="_blank" class="item-link">🔗</a>
            </div>
            <div class="item-right">
              <span class="item-price">{{ (item.price * item.quantity).toLocaleString() }}원</span>
              <button class="remove-btn" @click="removeItem(item.productCode)">×</button>
            </div>
          </li>
        </ul>
      </section>

      <!-- 3. 구매 완료 목록 (실제 지출) -->
      <section class="expense-list-card purchased-section">
        <h2 class="purchased-title">구매 완료 목록 ({{ purchasedItems.length }}개)</h2>
        
        <div v-if="purchasedItems.length === 0" class="empty-state">
          <p>아직 확정된 구매 내역이 없습니다.</p>
        </div>

        <ul v-else class="item-list">
          <li v-for="(item, index) in purchasedItems" :key="'purchased-'+index" class="item-row is-purchased">
            <div class="item-left">
              <span class="status-badge">구매완료</span>
              <span class="item-name">{{ item.name }}</span>
            </div>
            <div class="item-right">
              <span class="item-price">{{ item.price.toLocaleString() }}원</span>
              <button class="remove-btn" @click="removePurchasedItem(index)">×</button>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { globalState, removeFromCart } from "../utils/globalState";
import NnButton from "../components/common/NnButton.vue";

const cartItems = computed(() => globalState.cart);
const purchasedItems = computed(() => globalState.purchasedItems);

// 기본 예산 (로컬 상태)
const monthlyBudget = ref(500000); 
const isEditingBudget = ref(false);

// 지출 예정 금액 (장바구니)
const totalCartAmount = computed(() => {
  return cartItems.value.reduce(
    (total, item) => total + item.price * item.quantity,
    0,
  );
});

// 실제 지출 금액 (구매 확정)
const totalPurchasedAmount = computed(() => {
  return purchasedItems.value.reduce(
    (total, item) => total + item.price * (item.quantity || 1),
    0,
  );
});

const totalAmount = computed(() => totalCartAmount.value + totalPurchasedAmount.value);

const remainingBudget = computed(() => {
  return monthlyBudget.value - totalAmount.value;
});

const usagePercentage = computed(() => {
  if (monthlyBudget.value <= 0) return 100;
  return Math.min((totalAmount.value / monthlyBudget.value) * 100, 100);
});

function toggleBudgetEdit() {
  isEditingBudget.value = !isEditingBudget.value;
}

function removeItem(productCode) {
  removeFromCart(productCode);
}

// 구매 이력 삭제 (가계부에서만 관리용)
function removePurchasedItem(index) {
  if (confirm("이 구매 기록을 삭제하시겠습니까?")) {
    globalState.purchasedItems.splice(index, 1);
  }
}
</script>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 20px;
  font-family: 'Pretendard', sans-serif;
}

.page__header {
  text-align: center;
  margin-bottom: 40px;
}

.page__header h1 {
  font-size: 28px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
}

.page__header p {
  color: #6b7280;
  font-size: 16px;
}

.ledger-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Card Styles */
.budget-card, .expense-list-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

.budget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.budget-header h2, .expense-list-card h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.edit-btn {
  background: white;
  border: 1px solid #d1d5db;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #4b5563;
}

.budget-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  color: #4b5563;
  margin-bottom: 8px;
}

.budget-row.expense-actual {
  color: #374151;
  font-weight: 600;
}

.budget-row.expense {
  color: #ef4444;
}
...
.item-row.is-purchased {
  background-color: #f9fafb;
  padding: 12px 10px;
  border-radius: 8px;
  margin-bottom: 4px;
}

.status-badge {
  font-size: 10px;
  background-color: #374151;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
}

.purchased-section {
  border-top: 4px solid #f3f4f6;
  background-color: #fafafa;
}

.purchased-title {
  color: #6b7280 !important;
}

.budget-row.remaining {
  font-size: 20px;
  font-weight: 800;
  color: #10b981;
}

.budget-row.remaining .warning {
  color: #dc2626;
}

.divider {
  height: 1px;
  background-color: #e5e7eb;
  margin: 12px 0;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.input-wrapper input {
  flex: 1;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 16px;
  text-align: right;
}

/* Progress Bar */
.progress-container {
  margin-top: 24px;
}

.progress-bar {
  height: 12px;
  background-color: #f3f4f6;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background-color: #10b981;
  border-radius: 6px;
  transition: width 0.5s ease;
}

.progress-fill.over-budget {
  background-color: #ef4444;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

/* List */
.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f9fafb;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  flex: 1;
}

.item-name {
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.item-link {
  text-decoration: none;
  font-size: 14px;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-price {
  font-weight: 700;
  color: #1f2937;
}

.remove-btn {
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  font-size: 18px;
  padding: 0 4px;
}

.remove-btn:hover {
  color: #ef4444;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
}
</style>
