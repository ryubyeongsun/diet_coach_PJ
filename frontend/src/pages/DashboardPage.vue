<template>
  <div class="page dashboard-page">
    <div v-if="loading" class="loading-spinner">
      <p>대시보드를 불러오는 중입니다...</p>
    </div>
    <div v-else-if="error" class="error-banner">
      <p>{{ error }}</p>
    </div>
    <div v-else class="dashboard-re-layout">
      <div class="dashboard-main-content">
        <!-- 1. 캐릭터 메인 영역 -->
        <div class="character-area">
          <CharacterMain :summary="summaryData" :trend="trendData" :latest-weight="latestWeight" />
        </div>

        <!-- 2. 우측 요약 카드 영역 -->
        <div class="side-summary-area">
          <TodayIntakeCard :summary="summaryData" />
          <WeeklyWeightCard :trend="trendData" />
          <SystemTodoCard :summary="summaryData" />
        </div>
      </div>

      <!-- 3. 하단 보조 영역 -->
      <div class="bottom-aux-area">
        <TodayMealPreview :summary="summaryData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import CharacterMain from "../components/dashboard/CharacterMain.vue";
import TodayIntakeCard from "../components/dashboard/TodayIntakeCard.vue";
import WeeklyWeightCard from "../components/dashboard/WeeklyWeightCard.vue";
import SystemTodoCard from "../components/dashboard/SystemTodoCard.vue";
import TodayMealPreview from "../components/dashboard/TodayMealPreview.vue";
import { getDashboardSummary, getDashboardTrend } from "../api/dashboardApi";
import { getCurrentUser } from "../utils/auth";
import { format, subDays } from "date-fns";

const loading = ref(false);
const error = ref("");
const summaryData = ref(null);
const trendData = ref(null);
const currentUser = ref(getCurrentUser());

// Trend chart date range state
const trendDateRange = ref({
  from: format(subDays(new Date(), 29), "yyyy-MM-dd"),
  to: format(new Date(), "yyyy-MM-dd"),
});

const latestWeight = computed(() => {
  if (summaryData.value?.latestWeight) {
    return summaryData.value.latestWeight;
  }
  const sortedDays = trendData.value?.dayTrends
    ?.filter((d) => d.weight)
    .sort((a, b) => new Date(b.date) - new Date(a.date));
  return sortedDays?.[0]?.weight || currentUser.value?.weight;
});

async function fetchDashboardData() {
  if (!currentUser.value?.id) {
    error.value = "사용자 정보를 찾을 수 없습니다.";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const [summary, trend] = await Promise.all([
      getDashboardSummary(),
      getDashboardTrend({
        userId: currentUser.value.id,
        ...trendDateRange.value,
      }),
    ]);
    summaryData.value = summary;
    trendData.value = trend;
  } catch (err) {
    console.error("Failed to fetch dashboard data:", err);
    error.value = "대시보드 데이터를 불러오는 데 실패했습니다.";
    if (
      err.response?.config.url.includes("summary") &&
      err.response?.status === 404
    ) {
      error.value = "요약 정보를 불러올 수 없습니다. API 경로를 확인해주세요.";
    }
  } finally {
    loading.value = false;
  }
}

// This function can be used later if we add date controls back
async function fetchTrendData() {
  if (!currentUser.value?.id) return;
  try {
    trendData.value = await getDashboardTrend({
      userId: currentUser.value.id,
      ...trendDateRange.value,
    });
  } catch (err) {
    console.error("Failed to fetch trend data:", err);
  }
}

onMounted(() => {
  fetchDashboardData();
});
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  padding: 24px 32px;
  margin: 0 auto;
}

.dashboard-main-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  justify-content: center; /* Center the content */
}

.character-area {
  flex: 2; /* Takes up more space */
  min-width: 500px; /* Prevents it from getting too small */
}

.side-summary-area {
  flex: 1;
  max-width: 400px; /* Fixed max width */
  min-width: 350px; /* Prevents it from getting too small */
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.bottom-aux-area {
  margin-top: 32px; /* Space between main content and bottom */
}

.loading-spinner, .error-banner {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
  border-radius: 12px;
  background-color: rgba(255, 255, 255, 0.5);
}
.error-banner {
  background-color: #fef2f2;
  color: #dc2626;
  font-weight: 600;
  padding: 40px;
}

@media (max-width: 1024px) {
  .dashboard-page {
    padding: 24px;
  }
  .dashboard-main-content {
    flex-direction: column;
    align-items: stretch;
  }
  .character-area,
  .side-summary-area {
    min-width: 100%;
    max-width: 100%;
    flex: 1;
  }
}
</style>
