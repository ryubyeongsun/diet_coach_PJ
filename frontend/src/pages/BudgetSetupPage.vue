<template>
  <div class="page">
    <header class="page__header">
      <h1>예산 기반 장보기 설정</h1>
      <p class="subtitle">월 예산을 설정하면, 예산에 맞춰 최적의 장보기 리스트를 확정합니다.</p>
    </header>

    <div class="content">
      <!-- 입력 폼 -->
      <div class="setup-card">
        <h2>설정 입력</h2>
        <div class="form-group">
          <label>월 식비 예산 (원)</label>
          <NnInput 
            v-model="form.monthlyBudget" 
            type="number" 
            placeholder="예: 300000" 
          />
        </div>
        <div class="form-group">
          <label>하루 목표 칼로리 (kcal)</label>
          <NnInput 
            v-model="form.targetCalories" 
            type="number" 
            placeholder="예: 2000" 
          />
        </div>
        <div class="form-group">
          <label>끼니 수</label>
          <select v-model="form.mealsPerDay" class="nn-select">
            <option :value="3">3끼</option>
            <option :value="2">2끼</option>
          </select>
        </div>

        <NnButton 
          @click="generateProposal" 
          :disabled="isLoading" 
          fullWidth
        >
          {{ isLoading ? '계산 중...' : '장보기 리스트 확정하기' }}
        </NnButton>
      </div>

      <!-- 결과 화면 -->
      <div v-if="proposal" class="result-section">
        <div class="status-banner" :class="proposal.status">
          <h3>상태: {{ proposal.status === 'LOCKED' ? '확정됨 (LOCKED)' : proposal.status }}</h3>
          <p v-if="proposal.status === 'LOCKED'">예산 내에서 최적의 재료가 확정되었습니다. 이 재료로 식단을 생성합니다.</p>
        </div>

        <div class="cost-summary">
          <div class="cost-row">
            <span>월 예산</span>
            <span class="budget">{{ Number(proposal.budget).toLocaleString() }}원</span>
          </div>
          <div class="cost-row">
            <span>확정 비용</span>
            <span class="final-cost">{{ Number(proposal.finalCost).toLocaleString() }}원</span>
          </div>
          <div class="cost-row diff">
            <span>잔액</span>
            <span class="remaining">+{{ (proposal.budget - proposal.finalCost).toLocaleString() }}원</span>
          </div>
        </div>

        <div class="ingredient-list">
          <h3>확정된 장보기 목록</h3>
          <ul>
            <li v-for="(item, idx) in proposal.ingredients" :key="idx" class="ing-item">
              <img :src="item.imageUrl" alt="img" class="item-img" v-if="item.imageUrl"/>
              <div class="item-img-placeholder" v-else>💊</div>
              
              <div class="item-info">
                <div class="item-header">
                  <span class="sku-name">{{ item.skuName }}</span>
                  <span class="price">{{ Number(item.price).toLocaleString() }}원</span>
                </div>
                <div class="item-sub">
                  <span class="abstract-name">{{ item.name }}</span>
                  <span class="qty">x {{ item.quantity }}개</span>
                </div>
                <a :href="item.link" target="_blank" class="link">구매 링크 🔗</a>
              </div>
            </li>
          </ul>
        </div>

        <div class="actions">
          <NnButton variant="secondary" @click="createMealPlan" :disabled="isCreatingPlan">
            {{ isCreatingPlan ? '식단 생성 중...' : '이 재료로 식단 생성하기' }}
          </NnButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import NnInput from '../components/common/NnInput.vue';
import NnButton from '../components/common/NnButton.vue';
import { createBudgetProposal } from '../api/shoppingApi';
import { createMealPlan as createMealPlanApi } from '../api/mealPlanApi';
import { getCurrentUser } from '../utils/auth';

const router = useRouter();
const isLoading = ref(false);
const isCreatingPlan = ref(false); // New loading state
const proposal = ref(null);

const form = ref({
  monthlyBudget: 300000,
  targetCalories: 2000,
  mealsPerDay: 3
});

async function generateProposal() {
  const user = getCurrentUser();
  if (!user) {
    alert('로그인이 필요합니다.');
    router.push('/login');
    return;
  }

  isLoading.value = true;
  proposal.value = null;

  try {
    const payload = {
      userId: user.id,
      ...form.value,
      preferences: ["KOREAN"],
      allergies: []
    };
    
    const data = await createBudgetProposal(payload);
    proposal.value = data;
  } catch (err) {
    console.error(err);
    alert('장보기 리스트 생성에 실패했습니다.');
  } finally {
    isLoading.value = false;
  }
}

async function createMealPlan() {
  if (!proposal.value) return;
  const user = getCurrentUser();
  
  if (!confirm('확정된 재료로 한 달 식단을 생성하시겠습니까?\n기존 식단이 있다면 대체됩니다.')) return;

  isCreatingPlan.value = true;
  try {
    // Extract ingredient names (abstract names)
    const lockedIngredients = proposal.value.ingredients.map(i => i.name);
    
    // Default start date: Tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startDateStr = tomorrow.toISOString().split('T')[0];

    const payload = {
      userId: user.id,
      startDate: startDateStr,
      monthlyBudget: form.value.monthlyBudget,
      targetCalories: form.value.targetCalories,
      mealsPerDay: form.value.mealsPerDay,
      preferences: ["KOREAN"],
      allergies: [],
      lockedIngredients: lockedIngredients // Pass the locked list
    };

    await createMealPlanApi(payload);
    
    alert('식단 생성이 완료되었습니다!');
    router.push('/meal-plans');
  } catch (err) {
    console.error(err);
    alert('식단 생성 중 오류가 발생했습니다.');
  } finally {
    isCreatingPlan.value = false;
  }
}
</script>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page__header {
  text-align: center;
  margin-bottom: 32px;
}

.subtitle {
  color: #6b7280;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.setup-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  border: 1px solid #e5e7eb;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #374151;
}

.nn-select {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 16px;
}

/* Result Section */
.result-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.status-banner {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.status-banner.LOCKED {
  background-color: #ecfdf5;
  color: #065f46;
  border: 1px solid #10b981;
}

.status-banner.WARNING_OVER_BUDGET {
  background-color: #fffbeb;
  color: #92400e;
  border: 1px solid #f59e0b;
}

.cost-summary {
  background: white;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 16px;
}

.cost-row.diff {
  border-top: 1px solid #e5e7eb;
  margin-top: 8px;
  padding-top: 12px;
  font-weight: 800;
  color: #059669;
}

.ingredient-list {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.ingredient-list h3 {
  padding: 16px;
  margin: 0;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  font-size: 16px;
}

.ingredient-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ing-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  gap: 16px;
  align-items: center;
}

.item-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  background: #eee;
}

.item-img-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.item-info {
  flex: 1;
}

.item-header {
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  margin-bottom: 4px;
}

.item-sub {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.link {
  font-size: 12px;
  color: #2563eb;
  text-decoration: none;
}
</style>
