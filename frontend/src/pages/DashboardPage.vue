<template>
  <div class="page dashboard-page">
    <div v-if="loading" class="loading-spinner">
      <p>대시보드를 불러오는 중입니다...</p>
    </div>
    <div v-else-if="error" class="error-banner">
      <p>{{ error }}</p>
    </div>
    <div v-else class="dashboard-grid-layout">
      <!-- 1. Center Character Area -->
      <main class="character-area">
        <MainAvatarSection 
          :summary="summaryData" 
          :trend="trendData" 
          :latest-weight="latestWeight" 
          :user-height="userHeight"
        />
      </main>

      <!-- 2. Right Stats Area -->
      <aside class="stats-area">
        <TodayIntakeCard :summary="summaryData" />
        <WeeklyWeightCard :trend="trendData" />
        <SystemTodoCard :summary="summaryData" />
      </aside>

      <!-- 3. Bottom Aux Area -->
      <section class="bottom-area">
        <TodayMealPreview :summary="summaryData" @update="fetchDashboardData" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import MainAvatarSection from "../components/dashboard/MainAvatarSection.vue";
import TodayIntakeCard from "../components/dashboard/TodayIntakeCard.vue";
import WeeklyWeightCard from "../components/dashboard/WeeklyWeightCard.vue";
import SystemTodoCard from "../components/dashboard/SystemTodoCard.vue";
import TodayMealPreview from "../components/dashboard/TodayMealPreview.vue";
import { getDashboardSummary, getDashboardTrend } from "../api/dashboardApi";
import { fetchUserProfile } from "../api/usersApi";
import { getCurrentUser, updateCurrentUser } from "../utils/auth";
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

const userHeight = computed(() => currentUser.value?.height);

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
    const results = await Promise.allSettled([
      getDashboardSummary(),
      getDashboardTrend({
        userId: currentUser.value.id,
        ...trendDateRange.value,
      }),
      fetchUserProfile(currentUser.value.id)
    ]);

    if (results[0].status === 'fulfilled') {
      summaryData.value = results[0].value;
    }

    if (results[1].status === 'fulfilled') {
      trendData.value = results[1].value;
    }

    if (results[2].status === 'fulfilled' && results[2].value?.data) {
      updateCurrentUser(results[2].value.data);
      currentUser.value = results[2].value.data;
    }
  } catch (err) {
    console.error("Unexpected error in fetchDashboardData:", err);
    error.value = "데이터를 불러오는 중 예기치 않은 오류가 발생했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchDashboardData();
});
</script>

<style scoped>
.dashboard-page {
  /* Global background for dashboard */
  background: transparent; /* App.vue handles background */
  min-height: 100%;
  padding: 0; /* Remove padding as layout provides it */
  box-sizing: border-box;
}

.dashboard-grid-layout {
  display: grid;
  grid-template-columns: 1fr 320px; /* Sidebar removed from grid */
  grid-template-rows: auto auto; /* Main row + Bottom row */
  gap: 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.character-area {
  grid-column: 1 / 2;
  grid-row: 1 / 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stats-area {
  grid-column: 2 / 3;
  grid-row: 1 / 2;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bottom-area {
  grid-column: 1 / 3; /* Span full width */
  grid-row: 2 / 3;
}

/* Responsive Adjustments */
@media (max-width: 1024px) {
  .dashboard-grid-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto; /* Main, Stats, Bottom */
  }
  .character-area {
    grid-column: 1 / 2;
  }
  .stats-area {
    grid-column: 1 / 2;
    grid-row: 2 / 3;
  }
  .bottom-area {
    grid-column: 1 / 2;
    grid-row: 3 / 4;
  }
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
</style>
