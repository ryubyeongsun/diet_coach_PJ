<template>
  <div class="calendar">
    <div class="calendar__header">
      <h2>한 달 식단 요약</h2>
      <p class="calendar__subtitle">
        더블클릭하면 상세를 볼 수 있는 형태로 나중에 확장할 예정입니다.
      </p>
    </div>

    <div class="calendar__grid">
      <div
        v-for="day in days"
        :key="day.date"
        class="calendar__cell"
        @click="selectDay(day)"
      >
        <div class="calendar__date">{{ day.dateLabel }}</div>
        <div class="calendar__calories">
          {{ day.totalCalories }} kcal
        </div>
        <div class="calendar__meals">
          {{ day.mealSummary }}
        </div>
      </div>
    </div>

    <MealPlanDayDetail
      v-if="selectedDay"
      :day="selectedDay"
      class="calendar__detail"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import MealPlanDayDetail from './MealPlanDayDetail.vue';

// TODO: 나중에 백엔드 /api/meal-plans/{id}에서 받아올 데이터
const days = ref([
  {
    date: '2025-12-01',
    dateLabel: '12/01(월)',
    totalCalories: 1800,
    mealSummary: '아·점·저 균형식',
    meals: [
      { time: '아침', menu: '귀리 + 닭가슴살 샐러드', calories: 500 },
      { time: '점심', menu: '현미밥 + 제육볶음', calories: 700 },
      { time: '저녁', menu: '고구마 + 그릭요거트', calories: 600 },
    ],
  },
  {
    date: '2025-12-02',
    dateLabel: '12/02(화)',
    totalCalories: 1750,
    mealSummary: '탄수 조금 낮춤',
    meals: [
      { time: '아침', menu: '오트밀 + 바나나', calories: 450 },
      { time: '점심', menu: '현미밥 + 닭가슴살구이', calories: 750 },
      { time: '저녁', menu: '샐러드 + 삶은 달걀', calories: 550 },
    ],
  },
  // ... 더미로 몇 개 더 추가해도 됨
]);

const selectedDay = ref(null);

const selectDay = (day) => {
  selectedDay.value = day;
};
</script>

<style scoped>
.calendar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.calendar__header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.calendar__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.calendar__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.calendar__cell {
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.calendar__cell:hover {
  background-color: #eef2ff;
  border-color: #4f46e5;
}

.calendar__date {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
}

.calendar__calories {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.calendar__meals {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.calendar__detail {
  margin-top: 8px;
}
</style>
