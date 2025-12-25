<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>한 달 식단 플랜</h1>
        <p>TDEE와 예산을 기반으로 자동 생성될 식단의 레이아웃입니다.</p>
      </div>
      <div class="page__actions">
        <NnButton v-if="overview" variant="outline" @click="onClickGoShopping">
          이번 달 식단 재료 장보기
        </NnButton>
      </div>
    </header>

    <MealPlanCreatePanel
      ref="creationPanelRef"
      :user="currentUser"
      :is-loading="isLoading"
      :has-plan="!!overview"
      @create-plan="handleCreatePlan"
    />

    <p v-if="errorMessage" class="page__error">
      {{ errorMessage }}
    </p>

    <NnCard title="이번 달 식단 개요">
      <div v-if="isLoading" class="page__status">
        식단 정보를 불러오는 중입니다...
      </div>

      <div v-else-if="!overview" class="empty-state">
        <h3>아직 생성된 식단이 없습니다.</h3>
        <p>나만의 맞춤 식단을 생성하고 건강한 다이어트를 시작해 보세요.</p>
        <NnButton @click="scrollToCreatePanel" size="large">
          + 맞춤 식단 생성하기
        </NnButton>
      </div>
      
      <div v-else>
        <!-- 통계 대시보드 UI -->
        <div class="stat-board">
          <div class="stat-card">
            <span class="stat-card__label">기간 / 목표</span>
            <p class="stat-card__value">
              <strong>{{ overview.totalDays }}</strong
              >일 ·
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
        <MealPlanCalendar 
          :days="overview.days" 
          :target-calories="overview.targetCaloriesPerDay" 
          @click-day="onClickDay" 
        />
      </div>
    </NnCard>

    <MealPlanDayModal 
      v-model="isDayModalOpen" 
      :day-id="selectedDayId" 
      @stamp="handleStamp"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from "vue";
import { useRouter } from "vue-router";

import NnButton from "../components/common/NnButton.vue";
import NnCard from "../components/common/NnCard.vue";
import MealPlanCalendar from "../components/meal/MealPlanCalendar.vue";
import MealPlanCreatePanel from "../components/meal/MealPlanCreatePanel.vue";
import MealPlanDayModal from "../components/meal/MealPlanDayModal.vue";

import { fetchLatestMealPlan, createMealPlan, stampMealPlanDay } from "../api/mealPlanApi";
import { getCurrentUser } from "@/utils/auth.js";
import { getDashboardSummary, getDashboardTrend } from "../api/dashboardApi.js";

const isLoading = ref(false);
const errorMessage = ref("");
const currentUser = ref(null);
const overview = ref(null);
const router = useRouter();

// --- 상세 모달 상태 ---
const selectedDayId = ref(null);
const isDayModalOpen = ref(false);

// --- 폴링 상태 ---
let pollingInterval = null;

// --- 통계 데이터 계산 ---
const avgCalories = computed(() => {
  if (
    !overview.value ||
    !overview.value.days ||
    overview.value.days.length === 0
  ) {
    return 0;
  }
  const total = overview.value.days.reduce(
    (sum, day) => sum + day.totalCalories,
    0,
  );
  return Math.round(total / overview.value.days.length);
});

const achievementRate = computed(() => {
  if (
    !overview.value ||
    !overview.value.targetCaloriesPerDay ||
    avgCalories.value === 0
  ) {
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
    path: "/shopping",
    query: { planId: overview.value.mealPlanId },
  });
}

async function handleStamp(dayId) {
  if (!overview.value || !overview.value.days) return;
  
  try {
    // 1. 백엔드 DB 저장 (토글)
    await stampMealPlanDay(dayId);
    
    // 2. 서버 데이터 다시 불러오기 (UI 자동 갱신 및 통계 재계산)
    await loadLatest();
    
  } catch (err) {
    console.error("Failed to stamp day:", err);
    alert("작업에 실패했습니다. 다시 시도해 주세요.");
  }
}

// 식단 완성 여부 확인 (모든 날짜의 칼로리가 0보다 큰지)
function isPlanComplete(plan) {
  if (!plan || !plan.days) return false;
  return plan.days.every(day => day.totalCalories > 0);
}

function stopPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }
}

function startPolling(userId) {
  stopPolling(); // 기존 인터벌 제거
  
  // 3초마다 갱신
  pollingInterval = setInterval(async () => {
    try {
      const updatedPlan = await fetchLatestMealPlan(userId);
      if (updatedPlan) {
        overview.value = updatedPlan;
        // 완성이 되면 폴링 중단
        if (isPlanComplete(updatedPlan)) {
          stopPolling();
        }
      }
    } catch (err) {
      console.error("Polling error:", err);
      stopPolling(); // 에러 발생 시 중단
    }
  }, 3000);
}

async function loadLatest() {
  const userId = currentUser.value?.id;
  if (!userId) return;

  // 이미 데이터가 있고 폴링 중이라면 로딩 표시 스킵 가능하지만,
  // 초기 진입 시에는 로딩 표시
  if (!overview.value) {
    isLoading.value = true;
  }
  errorMessage.value = "";

  try {
    const plan = await fetchLatestMealPlan(userId);
    overview.value = plan;

    // 식단이 존재하지만 아직 미완성 상태라면 폴링 시작
    if (plan && !isPlanComplete(plan)) {
      startPolling(userId);
    }
  } catch (err) {
    console.error(err);
    if (
      err.response &&
      err.response.data &&
      err.response.data.message === "해당 사용자의 최근 식단 플랜이 없습니다."
    ) {
      overview.value = null;
    } else {
      errorMessage.value = "식단 정보를 불러오는 중 오류가 발생했습니다.";
    }
  } finally {
    isLoading.value = false;
  }
}

const creationPanelRef = ref(null);

function scrollToCreatePanel() {
  creationPanelRef.value?.scrollIntoView({ behavior: 'smooth' });
}

async function handleCreatePlan(payload) {
  if (!currentUser.value) {
    errorMessage.value = "사용자 정보가 필요합니다. 다시 로그인해주세요.";
    return;
  }

  isLoading.value = true;
  errorMessage.value = "";

  try {
    await createMealPlan({
      ...payload,
      userId: currentUser.value.id,
    });

    alert("식단이 성공적으로 생성되었습니다! AI가 식단을 짜는 동안 잠시만 기다려주세요.");

    // 식단 생성 직후 데이터 로드 및 폴링 시작
    await loadLatest();
  } catch (err) {
    console.error("Error creating meal plan:", err);
    if (err.response?.status === 400) {
      errorMessage.value = `입력값을 확인해주세요: ${err.response.data.message || ""}`;
    } else if (err.response?.status === 401) {
      router.push("/login");
    } else {
      errorMessage.value =
        "식단 생성 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    }
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  currentUser.value = getCurrentUser();
  if (currentUser.value) {
    loadLatest();
  }
});

onUnmounted(() => {
  stopPolling();
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

.empty-state {
  text-align: center;
  padding: 40px 20px;
  background-color: #f9fafb;
  border-radius: 8px;
}
.empty-state h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.empty-state p {
  margin: 0 0 16px;
  color: #6b7280;
}
</style>
