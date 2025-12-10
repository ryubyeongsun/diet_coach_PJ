<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h1>한 달 식단 플랜</h1>
        <p>
          TDEE와 예산을 기반으로 자동 생성될 식단의 레이아웃입니다.
          지금은 백엔드에서 받아온 데이터를 바인딩하고 있습니다.
        </p>

        <p v-if="currentUserId" class="page__user">
          현재 사용자 ID: {{ currentUserId }}
        </p>
      </div>
      <div class="page__actions">
        <NnButton
          v-if="overview"
          secondary
          @click="onClickGoShopping"
        >
          이 식단 재료 장보기 (준비중)
        </NnButton>
        <NnButton
          block
          :disabled="isLoading || !currentUserId"
          @click="onClickGenerate"
        >
          {{ isLoading ? '처리 중...' : '식단 자동 생성' }}
        </NnButton>
      </div>
    </header>

    <p v-if="errorMessage" class="page__error">
      {{ errorMessage }}
    </p>

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
		<MealPlanCalendar :days="overview.days" />
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';

import NnButton from '../components/common/NnButton.vue';
import NnCard from '../components/common/NnCard.vue';
import MealPlanCalendar from '../components/meal/MealPlanCalendar.vue';

import { fetchLatestMealPlan, generateMealPlan } from '../api/mealPlanApi';
import { createUser } from '../api/usersApi.js'; // Import createUser

const isLoading = ref(false);        // 식단 불러오기/생성 로딩
const errorMessage = ref('');        // 식단 관련 에러
const currentUserId = ref(null);     // 현재 사용 중인 유저 ID
const overview = ref(null);          // MealPlanOverviewResponse
const router = useRouter();

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

function onClickGoShopping() {
  router.push('/shopping');
}

async function initUser() {
  let storedId = localStorage.getItem('userId');
  if (storedId) {
    currentUserId.value = Number(storedId);
    console.log('[MealPlanPage] Loaded userId from localStorage:', currentUserId.value);
    return;
  }

  try {
    const uniqueEmail = `test-user-${Date.now()}@example.com`;
    const newUserId = await createUser({
      email: uniqueEmail,
      password: '1234',
      name: '테스트유저',
      gender: 'MALE',
      birthDate: '1998-01-01',
      height: 175,
      weight: 70,
      activityLevel: 'MODERATE',
      goalType: 'LOSE_WEIGHT',
    });

    // The API returns the ID directly as a number.
    if (newUserId) {
      currentUserId.value = newUserId;
      localStorage.setItem('userId', String(newUserId));
      console.log('[MealPlanPage] Created new user with ID:', currentUserId.value);
    } else {
      errorMessage.value = '사용자 생성 후 서버로부터 올바른 ID를 받지 못했습니다.';
      console.error('Unexpected response from createUser (expected a user ID):', newUserId);
    }
  } catch (err) {
    console.error('[MealPlanPage] createUser error:', err);
    errorMessage.value = '사용자 생성 중 오류가 발생했습니다.';
  }
}

async function loadLatest() {
  if (!currentUserId.value) return;

  isLoading.value = true;
  errorMessage.value = '';

  try {
    overview.value = await fetchLatestMealPlan(currentUserId.value);
  } catch (err) {
    console.error(err);
    // Check for specific backend message indicating no meal plan
    if (err.response && err.response.data && err.response.data.message === '해당 유저의 최근 식단 플랜이 없습니다.') {
      overview.value = null; // No meal plan, display "아직 생성된 식단이 없습니다."
    } else {
      errorMessage.value = '식단 정보를 불러오는 중 오류가 발생했습니다.';
    }
  } finally {
    isLoading.value = false;
  }
}

async function onClickGenerate() {
  if (!currentUserId.value) {
    errorMessage.value = '사용자 정보가 준비되지 않았습니다.';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    const today = new Date().toISOString().slice(0, 10);
    await generateMealPlan({
      userId: currentUserId.value,
      startDate: today,
      totalDays: 30,
    });

    // 새로 생성 후 최신 식단 다시 로드
    await loadLatest();
  } catch (err) {
    console.error(err);
    errorMessage.value = '식단 생성 중 오류가 발생했습니다.';
  } finally {
    isLoading.value = false;
  }
}

onMounted(async () => {
  await initUser();
  if (currentUserId.value) {
    await loadLatest();
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
  gap: 16px;
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
  font-size: 13px;
  color: #6b7280;
}

.page__summary {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
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
