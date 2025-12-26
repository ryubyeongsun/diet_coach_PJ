<template>
  <div class="card meal-preview-card">
    <div class="header">
      <h2 class="section-title">오늘의 추천 식단</h2>
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

    <div v-else class="meal-slider-container">
      <div class="meal-list hide-scrollbar">
        <div 
          v-for="meal in mealList" 
          :key="meal.mealTime" 
          class="meal-card"
          :class="{ 'is-consumed': meal.isConsumed }"
        >
          <div class="card-top">
            <span class="badge" :class="getBadgeClass(meal.mealTime)">
              {{ formatMealTime(meal.mealTime) }}
            </span>
            <span class="kcal">{{ meal.totalCalories }} kcal</span>
          </div>
          
          <div class="card-body">
            <div class="menu-text" :title="getMenuString(meal.items)">
              {{ formatMenu(meal.items) }}
            </div>
          </div>

          <div class="card-footer">
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
              {{ meal.isConsumed ? '완료' : '미완료' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Celebration Modal (Simple & Bold) -->
    <Teleport to="body">
      <div v-if="showCelebration" class="celebration-overlay" @click="showCelebration = false">
        <div class="celebration-content">
          <h1 class="celebrate-title-big">목표 달성!</h1>
          <p class="celebrate-msg-big">오늘 하루도 건강하게 채우셨군요 ✨</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { fetchDayDetail, upsertIntake } from '../../api/mealPlanApi';
import { getCurrentUser } from '../../utils/auth';
import confetti from 'canvas-confetti';

const props = defineProps({
  summary: Object,
});

const emit = defineEmits(['update']);

const loading = ref(false);
const dayDetail = ref(null);
const processingMap = ref({}); // mealTime -> boolean
const showCelebration = ref(false);

const hasData = computed(() => !!dayDetail.value);

// Backwards compatibility for API response (meals vs items)
const mealList = computed(() => {
  if (!dayDetail.value) return [];
  
  // If backend returns 'meals' (new structure)
  if (dayDetail.value.meals && dayDetail.value.meals.length > 0) {
    return dayDetail.value.meals;
  }
  
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

function getMenuString(items) {
  if (!items) return '';
  return items.map(i => i.foodName).join(', ');
}

function formatMenu(items) {
  if (!items || items.length === 0) return '메뉴 없음';
  const names = items.map(i => i.foodName);
  // Show more items in card format
  if (names.length <= 3) return names.join(', ');
  return `${names[0]}, ${names[1]} 외 ${names.length - 2}개`;
}

function isProcessing(mealTime) {
  return !!processingMap.value[mealTime];
}

async function onToggle(meal) {
  if (!user || !user.id || !props.summary?.todayDayId) return;
  
  const mealTime = meal.mealTime;
  processingMap.value[mealTime] = true;
  
  const targetState = meal.isConsumed;

  try {
    await upsertIntake({
      userId: user.id,
      mealPlanDayId: props.summary.todayDayId,
      mealTime: mealTime,
      isConsumed: targetState
    });
    
    const isAllConsumed = targetState && mealList.value.every(m => m.isConsumed);

    if (isAllConsumed) {
      triggerConfetti();
      showCelebration.value = true;
      
      setTimeout(() => {
        showCelebration.value = false;
        emit('update'); 
      }, 2500);
    } else {
      emit('update'); 
    }

  } catch (e) {
    console.error("Failed to update intake:", e);
    meal.isConsumed = !targetState;
    alert("상태 업데이트에 실패했습니다.");
  } finally {
    processingMap.value[mealTime] = false;
  }
}

function triggerConfetti() {
  const duration = 2500;
  const end = Date.now() + duration;

  (function frame() {
    confetti({
      particleCount: 3,
      angle: 60,
      spread: 55,
      origin: { x: 0 },
      colors: ['#10b981', '#34d399', '#f59e0b']
    });
    confetti({
      particleCount: 3,
      angle: 120,
      spread: 55,
      origin: { x: 1 },
      colors: ['#10b981', '#34d399', '#f59e0b']
    });

    if (Date.now() < end) {
      requestAnimationFrame(frame);
    }
  }());
}
</script>

<style scoped>
/* Celebration Modal Styles */
.celebration-overlay {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  background-color: rgba(0, 0, 0, 0.85) !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  z-index: 2147483647 !important;
  backdrop-filter: blur(5px);
}

.celebration-content {
  text-align: center;
  color: white;
  animation: zoomIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.celebrate-title-big {
  font-size: 48px;
  font-weight: 900;
  color: #10b981;
  margin: 0 0 16px 0;
  text-shadow: 0 0 20px rgba(16, 185, 129, 0.6);
  letter-spacing: -1px;
}

.celebrate-msg-big {
  font-size: 20px;
  color: #e5e7eb;
  font-weight: 500;
}

@keyframes zoomIn {
  from { transform: scale(0.5); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* Card Styles */
.meal-preview-card {
  position: relative;
  background: transparent; /* Seamless blend */
  padding: 0; /* Let cards define padding */
  border-radius: 0;
  box-shadow: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
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

/* Horizontal Scroll Container */
.meal-slider-container {
  width: 100%;
  overflow: hidden; /* Hide outer overflow */
}

.meal-list {
  display: flex;
  flex-direction: row; /* Horizontal */
  gap: 16px;
  overflow-x: auto;
  padding: 4px 4px 16px 4px; /* Bottom padding for shadow */
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

/* Hide Scrollbar Utility */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* Individual Meal Card */
.meal-card {
  min-width: 220px;
  width: 220px;
  background: white;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 2px solid transparent;
}

.meal-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

.meal-card.is-consumed {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
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

.card-body {
  flex: 1;
  margin-bottom: 16px;
}

.menu-text {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  line-height: 1.4;
  word-break: keep-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #e5e7eb;
  transition: .4s;
  border-radius: 34px;
}
.slider:before {
  position: absolute;
  content: "";
  height: 18px; width: 18px;
  left: 3px; bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}
input:checked + .slider { background-color: #10b981; }
input:checked + .slider:before { transform: translateX(20px); }

.status-text {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 600;
}
.status-text.active { color: #10b981; }

/* Empty State */
.state-msg.empty {
  background-color: white;
  border: 2px dashed #bbf7d0;
  border-radius: 20px;
  padding: 40px;
  text-align: center;
}
.cute-mascot { font-size: 48px; margin-bottom: 12px; }
.cute-text { font-size: 18px; font-weight: 700; color: #047857; margin-bottom: 8px; }
.sub-text { font-size: 14px; color: #6b7280; margin-top: 4px; line-height: 1.5; }
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
.go-plan-btn:hover { transform: scale(1.05); background-color: #059669; }
</style>