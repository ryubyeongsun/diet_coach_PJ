<template>
  <div class="calendar">
    <!-- 상단 달성률 게이지 -->
    <div v-if="days.length > 0 && targetCalories > 0" class="calendar__progress-section">
      <div class="progress-info">
        <span class="progress-label">이번 달 목표 달성률</span>
        <span class="progress-percent">{{ monthlyAchievementRate }}%</span>
      </div>
      <div class="progress-bar-bg">
        <div
          class="progress-bar-fill"
          :style="{ width: Math.min(monthlyAchievementRate, 100) + '%' }"
        ></div>
      </div>
    </div>

    <!-- 데이터 없을 때 -->
    <div v-if="!days || days.length === 0" class="calendar__empty">
      아직 생성된 식단이 없습니다. 상단에서 식단을 먼저 생성해 주세요.
    </div>

    <!-- 데이터 있을 때 -->
    <div v-else class="calendar__list">
      <button
        v-for="day in days"
        :key="day.dayId"
        type="button"
        class="calendar__item"
        :class="getStatusClass(day)"
        @click="onClickDay(day)"
      >
        <p class="calendar__date">
          {{ formatDate(day.date) }}
        </p>
        <p class="calendar__kcal">
          {{ day.totalCalories != null ? day.totalCalories : "-" }} kcal
        </p>
        <!-- 대표 메뉴 표시 -->
        <p class="calendar__menu">
          {{ getRepresentativeMenu(day) }}
        </p>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  days: {
    type: Array,
    required: true,
  },
  targetCalories: {
    type: Number,
    default: 2000,
  },
});

const emit = defineEmits(["click-day"]);

const weekday = ["일", "월", "화", "수", "목", "금", "토"];

// 월간 달성률 계산
const monthlyAchievementRate = computed(() => {
  if (!props.days.length || !props.targetCalories) return 0;
  
  const totalCal = props.days.reduce((sum, day) => sum + (day.totalCalories || 0), 0);
  const totalTarget = props.targetCalories * props.days.length;
  
  return Math.round((totalCal / totalTarget) * 100);
});

function formatDate(isoString) {
  if (!isoString) return "";
  const d = new Date(isoString);
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const w = weekday[d.getDay()];
  return `${m}/${day}(${w})`;
}

function onClickDay(day) {
  emit("click-day", day);
}

// 상태별 클래스 반환 (초록: ±5% 이내, 주황: 그 외)
function getStatusClass(day) {
  if (!day.totalCalories || !props.targetCalories) return "";
  
  const ratio = day.totalCalories / props.targetCalories;
  // 0.95 ~ 1.05 사이면 성공
  if (ratio >= 0.95 && ratio <= 1.05) {
    return "calendar__item--success";
  }
  return "calendar__item--warning";
}

// 대표 메뉴 추출 (칼로리가 가장 높은 항목)
function getRepresentativeMenu(day) {
  if (day.representativeMenu) return day.representativeMenu;
  return "식단 미정";
}
</script>

<style scoped>
.calendar {
  width: 100%;
}

.calendar__progress-section {
  margin-bottom: 20px;
  background-color: #f9fafb;
  padding: 12px 16px;
  border-radius: 12px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.progress-percent {
  color: #2563eb;
}

.progress-bar-bg {
  width: 100%;
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background-color: #3b82f6; /* Blue-500 */
  border-radius: 4px;
  transition: width 0.3s ease;
}

.calendar__empty {
  padding: 20px 16px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.calendar__list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.calendar__item {
  flex: 0 0 calc(25% - 9px); /* 4개씩 */
  min-width: 160px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); /* 그림자 약간 줄임 */
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.calendar__item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

/* Success State (Green) */
.calendar__item--success {
  border-color: #86efac; /* Green-300 */
  background-color: #f0fdf4; /* Green-50 */
}
.calendar__item--success .calendar__kcal {
  color: #15803d; /* Green-700 */
}

/* Warning State (Orange/Yellow) */
.calendar__item--warning {
  border-color: #fdba74; /* Orange-300 */
  background-color: #fff7ed; /* Orange-50 */
}
.calendar__item--warning .calendar__kcal {
  color: #c2410c; /* Orange-700 */
}

.calendar__date {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
}

.calendar__kcal {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

.calendar__menu {
  margin: 0;
  font-size: 13px;
  color: #374151;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
