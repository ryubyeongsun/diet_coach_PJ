<template>
  <div class="card meal-preview-card">
    <div class="header">
      <h2 class="section-title">오늘의 식단</h2>
      <button v-if="hasData" class="refresh-btn" @click="fetchData" title="새로고침">
         ↻
      </button>
    </div>

      <div v-if="isLoading" class="state-msg">
        <div class="spinner"></div> 남남코치가 식단 불러오는 중... 🥑
      </div>

    <div v-else-if="!hasData" class="state-msg empty">
      <div class="cute-mascot">🥑</div>
      <p class="cute-text">앗! 아직 식단이 없어요.</p>
      <p class="sub-text">나만의 맞춤 식단을 생성하고<br/>건강한 하루를 시작해볼까요? ✨</p>
      <button class="go-plan-btn" @click="$router.push('/meal-plans')">식단 만들러 가기</button>
    </div>

    <div v-else class="meal-list">
      <div 
        v-for="meal in mealList" 
        :key="meal.mealTime" 
        class="meal-row"
        :class="{ 'is-consumed': meal.isConsumed }"
      >
        <div class="meal-info">
          <div class="badge-row">
            <span class="badge" :class="getBadgeClass(meal.mealTime)">
              {{ formatMealTime(meal.mealTime) }}
            </span>
            <span class="kcal">{{ meal.totalCalories }} kcal</span>
          </div>
          <div class="menu-text">
            {{ formatMenu(meal.items) }}
          </div>
        </div>

        <div class="action-area">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              v-model="meal.isConsumed"
              @change="onToggle(meal)"
              :disabled="isProcessing(meal.mealTime)"
            />
            <span class="slider round"></span>
          </label>
          <span class="status-text" :class="{ active: meal.isConsumed }">
            {{ meal.isConsumed ? '먹었어요' : '안 먹음' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { fetchDayDetail, upsertIntake } from '../../api/mealPlanApi';
import { getCurrentUser } from '../../utils/auth';

const props = defineProps({
  summary: Object,
});

const emit = defineEmits(['update']);

const loading = ref(false);
const dayDetail = ref(null);
const processingMap = ref({}); // mealTime -> boolean

const hasData = computed(() => !!dayDetail.value);

// Backwards compatibility for API response (meals vs items)
const mealList = computed(() => {
  if (!dayDetail.value) return [];
  
  // If backend returns 'meals' (new structure)
  if (dayDetail.value.meals && dayDetail.value.meals.length > 0) {
    return dayDetail.value.meals;
  }
  
  // Fallback if backend returns flat 'items' (old structure or failed migration)
  // We can group them manually if needed, but for now let's hope backend works.
  // If only items exist, we might not have isConsumed status properly unless we fetch intakes separately.
  // But our backend change guarantees 'meals' field.
  return []; 
});

const user = getCurrentUser();

watch(() => props.summary?.todayDayId, (newId) => {
  if (newId) {
    fetchData();
  } else {
    dayDetail.value = null;
  }
}, { immediate: true });

async function fetchData() {
  const dayId = props.summary?.todayDayId;
  if (!dayId) return;

  loading.value = true;
  try {
    const res = await fetchDayDetail(dayId);
    dayDetail.value = res;
  } catch (err) {
    console.error("Failed to fetch day detail:", err);
  } finally {
    loading.value = false;
  }
}

function formatMealTime(t) {
  const map = { BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식' };
  return map[t] || t;
}

function getBadgeClass(t) {
  const map = { BREAKFAST: 'bg-morning', LUNCH: 'bg-lunch', DINNER: 'bg-dinner', SNACK: 'bg-snack' };
  return map[t] || 'bg-gray';
}

function formatMenu(items) {
  if (!items || items.length === 0) return '메뉴 없음';
  // Show first 2 items and count
  const names = items.map(i => i.foodName);
  if (names.length <= 2) return names.join(', ');
  return `${names[0]}, ${names[1]} 외 ${names.length - 2}개`;
}

function isProcessing(mealTime) {
  return !!processingMap.value[mealTime];
}

async function onToggle(meal) {
  if (!user || !user.id || !props.summary?.todayDayId) return;
  
  const mealTime = meal.mealTime;
  processingMap.value[mealTime] = true;
  
  // v-model has already updated meal.isConsumed to the new state (target state)
  const targetState = meal.isConsumed;

  try {
    await upsertIntake({
      userId: user.id,
      mealPlanDayId: props.summary.todayDayId,
      mealTime: mealTime,
      isConsumed: targetState
    });
    // Emit update so parent can refresh summary (calories)
    emit('update'); 
  } catch (e) {
    console.error("Failed to update intake:", e);
    // Revert on failure
    meal.isConsumed = !targetState;
    alert("상태 업데이트에 실패했습니다.");
  } finally {
    processingMap.value[mealTime] = false;
  }
}
</script>

<style scoped>
.meal-preview-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.refresh-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #6b7280;
}
.refresh-btn:hover { color: #374151; }

.state-msg {
  text-align: center;
  padding: 40px 0;
  color: #6b7280;
}
.state-msg.empty {
  background-color: #f0fdf4;
  border: 2px dashed #bbf7d0;
  border-radius: 16px;
  padding: 40px 20px;
}
.cute-mascot {
  font-size: 48px;
  margin-bottom: 12px;
  animation: bounce 2s infinite;
}
.cute-text {
  font-size: 18px;
  font-weight: 700;
  color: #047857;
  margin-bottom: 8px;
}
.sub-text {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
  line-height: 1.5;
}
.go-plan-btn {
  margin-top: 20px;
  padding: 10px 24px;
  background-color: #047857;
  color: white;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}
.go-plan-btn:hover {
  transform: scale(1.05);
  background-color: #059669;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.meal-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.meal-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: all 0.2s ease;
}
.meal-row.is-consumed {
  background-color: #f0fdf4; /* Light green bg */
  border-color: #bbf7d0;
}

.meal-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
.bg-morning { background-color: #f59e0b; }
.bg-lunch { background-color: #10b981; }
.bg-dinner { background-color: #3b82f6; }
.bg-snack { background-color: #8b5cf6; }
.bg-gray { background-color: #9ca3af; }

.kcal {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
}

.menu-text {
  font-size: 15px;
  font-weight: 500;
  color: #374151;
}

/* Toggle Switch */
.action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 60px;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 26px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #e5e7eb;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
}

input:checked + .slider {
  background-color: #10b981;
}

input:focus + .slider {
  box-shadow: 0 0 1px #10b981;
}

input:checked + .slider:before {
  transform: translateX(22px);
}

.slider.round {
  border-radius: 34px;
}

.slider.round:before {
  border-radius: 50%;
}

.status-text {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 600;
}
.status-text.active {
  color: #10b981;
}

.spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid #ccc;
  border-top-color: #333;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>