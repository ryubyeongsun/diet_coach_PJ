<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>한 달 식단 플랜</h1>
        <p>
          TDEE와 예산을 기반으로 자동 생성될 식단의 레이아웃입니다.
        </p>

        <p v-if="currentUser" class="page__user">
          현재 사용자 ID: {{ currentUser.id }}
        </p>
      </div>
      <div class="page__actions">
        <NnButton
          v-if="overview"
          variant="outline"
          @click="onClickGoShopping"
        >
          이번 달 식단 재료 장보기
        </NnButton>
      </div>
    </header>

    <MealPlanCreatePanel 
      :user="currentUser"
      :is-loading="isLoading"
      :has-plan="!!overview"
      @create-plan="handleCreatePlan"
    />

    <p v-if="errorMessage" class="page__error">
      {{ errorMessage }}
    </p>

    <!-- 대시보드 요약 -->
    <p v-if="dashboardError" class="page__error">{{ dashboardError }}</p>
    <section class="dashboard-summary" v-if="dashboard">
      <NnCard>
        <div class="dashboard-summary__row">
          <div>
            <div class="dashboard-summary__label">현재 체중</div>
            <div class="dashboard-summary__value">
              {{ dashboard.latestWeight }} kg
            </div>
          </div>
          <div>
            <div class="dashboard-summary__label">7일 변화</div>
            <div class="dashboard-summary__value">
              <span v-if="dashboard.weightChange7Days > 0" class="positive">
                +{{ dashboard.weightChange7Days }} kg
              </span>
              <span v-else-if="dashboard.weightChange7Days < 0" class="negative">
                {{ dashboard.weightChange7Days }} kg
              </span>
              <span v-else>
                {{ dashboard.weightChange7Days }} kg
              </span>
            </div>
          </div>
          <div>
            <div class="dashboard-summary__label">목표 칼로리</div>
            <div class="dashboard-summary__value">
              {{ dashboard.targetCalories }} kcal
            </div>
          </div>
          <div>
            <div class="dashboard-summary__label">최근 평균 섭취량</div>
            <div class="dashboard-summary__value">
              {{ dashboard.averageIntake }} kcal
            </div>
          </div>
        </div>
      </NnCard>
    </section>



    <!-- 트렌드 차트 -->
    <section class="trend-section">
      <div class="trend-header">
        <h2>성능 트렌드</h2>
        <div class="period-selector">
          <button @click="setPeriod(7)" :class="{ active: period === 7 }">7일</button>
          <button @click="setPeriod(30)" :class="{ active: period === 30 }">30일</button>
        </div>
      </div>
      <div v-if="isTrendLoading" class="page__status">
        트렌드 데이터를 불러오는 중입니다...
      </div>
      <div v-else-if="trendError" class="page__error">
        {{ trendError }}
      </div>
      <div v-else-if="!trend || trend.dayTrends.length < 2" class="page__status">
        그래프를 표시하려면 최소 2일 이상의 체중 기록이 필요합니다. <br> 꾸준히 기록해주세요!
      </div>
      <TrendChart
        v-else-if="trend"
        :day-trends="trend.dayTrends"
        :period-label="`최근 ${period}일`"
      />
    </section>

    <NnCard title="이번 달 식단 개요">
      <div v-if="isLoading" class="page__status">
        식단 정보를 불러오는 중입니다...
      </div>

      <div v-else-if="!overview">
        아직 생성된 식단이 없습니다. 상단 버튼을 눌러 식단을 생성해 보세요.
      </div>

      <div v-else>
        <!-- 통계 대시보드 UI -->
        <div class="stat-board">
          <div class="stat-card">
            <span class="stat-card__label">기간 / 목표</span>
            <p class="stat-card__value">
              <strong>{{ overview.totalDays }}</strong>일 · 
              <strong>{{ overview.targetCaloriesPerDay }}</strong>
              <span class="stat-card__unit">kcal</span>
            </p>
          </div>
          <div class="stat-card">
            <span class="stat-card__label">평균 섭취량</span>
            <p class="stat-card__value">
              <strong>{{ avgCalories }}</strong>
              <span class="stat-card__unit">kcal</span>
            </p>
          </div>
          <div class="stat-card">
            <span class="stat-card__label">목표 달성률</span>
            <p class="stat-card__value">
              <strong>{{ achievementRate }}</strong>
              <span class="stat-card__unit">%</span>
            </p>
          </div>
        </div>

		<!-- ⭐ 실제 데이터 내려주기 -->
		<MealPlanCalendar :days="overview.days" @click-day="onClickDay" />
      </div>
    </NnCard>

    <MealPlanDayModal v-model="isDayModalOpen" :day-id="selectedDayId" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';

import NnButton from '../components/common/NnButton.vue';
import NnCard from '../components/common/NnCard.vue';
import MealPlanCalendar from '../components/meal/MealPlanCalendar.vue';
import MealPlanCreatePanel from '../components/meal/MealPlanCreatePanel.vue';
import MealPlanDayModal from '../components/meal/MealPlanDayModal.vue';
import TrendChart from '../components/dashboard/TrendChart.vue';

import { fetchLatestMealPlan, createMealPlan } from '../api/mealPlanApi';
import { getCurrentUser } from '@/utils/auth.js';
import { fetchDashboardSummary, fetchDashboardTrend } from '../api/dashboardApi.js';

const isLoading = ref(false);
const errorMessage = ref('');
const currentUser = ref(null);
const overview = ref(null);
const router = useRouter();

// --- 대시보드 ---

const dashboard = ref(null);
const dashboardError = ref('');

// --- 트렌드 ---
const trend = ref(null);
const isTrendLoading = ref(false);
const trendError = ref('');
const period = ref(7); // 7일 또는 30일

// --- 상세 모달 상태 ---
const selectedDayId = ref(null);
const isDayModalOpen = ref(false);

// --- 통계 데이터 계산 ---
const avgCalories = computed(() => {
  if (!overview.value || !overview.value.days || overview.value.days.length === 0) {
    return 0;
  }
  const total = overview.value.days.reduce((sum, day) => sum + day.totalCalories, 0);
  return Math.round(total / overview.value.days.length);
});

const achievementRate = computed(() => {
  if (!overview.value || !overview.value.targetCaloriesPerDay || avgCalories.value === 0) {
    return 0;
  }
  const rate = (avgCalories.value / overview.value.targetCaloriesPerDay) * 100;
  return Math.round(rate);
});

function onClickDay(day) {
  if (!day || !day.dayId) return;
  selectedDayId.value = day.dayId;
  isDayModalOpen.value = true;
}

function onClickGoShopping() {
  if (!overview.value || !overview.value.mealPlanId) return;
  router.push({
    path: '/shopping',
    query: { planId: overview.value.mealPlanId },
  });
}

async function loadLatest() {
  const userId = currentUser.value?.id;
  if (!userId) return;

  isLoading.value = true;
  errorMessage.value = '';

  try {
    overview.value = await fetchLatestMealPlan(userId);
  } catch (err) {
    console.error(err);
    if (err.response && err.response.data && err.response.data.message === '해당 유저의 최근 식단 플랜이 없습니다.') {
      overview.value = null;
    } else {
      errorMessage.value = '식단 정보를 불러오는 중 오류가 발생했습니다.';
    }
  } finally {
    isLoading.value = false;
  }
}

async function loadDashboardSummary() {
  const userId = currentUser.value?.id;
  if (!userId) return;
  try {
    dashboardError.value = '';
    dashboard.value = await fetchDashboardSummary(userId);
  } catch (err) {
    console.error(err);
    dashboardError.value = '대시보드 정보를 불러오는 중 오류가 발생했습니다.';
  }
}

async function loadTrendData() {
  const userId = currentUser.value?.id;
  if (!userId) return;
  isTrendLoading.value = true;
  trendError.value = '';
  try {
    const today = new Date();
    const fromDate = new Date();
    fromDate.setDate(today.getDate() - (period.value - 1));
    
    const to = today.toISOString().slice(0, 10);
    const from = fromDate.toISOString().slice(0, 10);

    trend.value = await fetchDashboardTrend(userId, from, to);
  } catch (err) {
    console.error('Trend API error:', err);
    trendError.value = '트렌드 데이터를 불러오는 중 오류가 발생했습니다.';
  } finally {
    isTrendLoading.value = false;
  }
}

// `period` 값이 변경되면 트렌드 데이터를 다시 로드합니다.
watch(period, loadTrendData);

// 버튼 클릭 시 `period` 값만 변경합니다. 데이터 로딩은 watch가 처리합니다.
function setPeriod(days) {
  period.value = days;
}

async function handleCreatePlan(payload) {
  if (!currentUser.value) {
    errorMessage.value = '사용자 정보가 필요합니다. 다시 로그인해주세요.';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    await createMealPlan({
      ...payload,
      userId: currentUser.value.id,
    });

    alert('식단이 성공적으로 생성되었습니다!');

    // 식단 생성 후에는 모든 관련 데이터를 다시 불러옵니다.
    await Promise.all([loadLatest(), loadDashboardSummary(), loadTrendData()]);

  } catch (err) {
    console.error('Error creating meal plan:', err);
    if (err.response?.status === 400) {
      errorMessage.value = `입력값을 확인해주세요: ${err.response.data.message || ''}`;
    } else if (err.response?.status === 401) {
      router.push('/login');
    } else {
      errorMessage.value = '식단 생성 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    }
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  currentUser.value = getCurrentUser();
  if (currentUser.value) {
    // 최초 로딩 시 모든 데이터를 불러옵니다.
    // loadTrendData는 period의 초기값(7)에 대해 자동으로 호출됩니다.
    loadLatest();
    loadDashboardSummary();
    loadTrendData();
  }
});
</script>


<style scoped>
/* ... existing styles ... */
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
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

.page__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page__user {
  margin-top: 6px;
  font-size: 12px;
  color: #4b5563;
}

.page__error {
  margin: 0;
  font-size: 13px;
  color: #dc2626;
}

.page__status {
  padding: 16px 0;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

/* --- 대시보드 요약 --- */
.dashboard-summary {
  margin-bottom: -8px;
}
.dashboard-summary__row {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  text-align: center;
}
.dashboard-summary__label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}
.dashboard-summary__value {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}
.dashboard-summary__value .positive {
  color: #dc2626;
}
.dashboard-summary__value .negative {
  color: #166534;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.trend-header h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
.period-selector button {
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  color: #374151;
  cursor: pointer;
}
.period-selector button:first-child {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}
.period-selector button:last-child {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
  border-left: none;
}
.period-selector button.active {
  background-color: #e5e7eb;
  font-weight: 600;
}


/* --- 통계 대시보드 스타일 --- */
.stat-board {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  background-color: #f3f4f6;
  border-radius: 8px;
  padding: 12px;
}

.stat-card__label {
  font-size: 12px;
  color: #4b5563;
}

.stat-card__value {
  margin: 4px 0 0;
  font-size: 16px;
  color: #1f2937;
}

.stat-card__value strong {
  font-size: 20px;
  font-weight: 700;
}

.stat-card__unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 2px;
}
</style>
