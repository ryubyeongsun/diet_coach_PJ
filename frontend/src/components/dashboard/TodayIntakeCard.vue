<template>
  <div class="card summary-card">
    <h3>오늘 섭취 칼로리</h3>
    <div v-if="summary">
      <p class="calories">
        {{ (summary.consumedCalories ?? 0).toLocaleString() }}
        <span class="unit">/ {{ (summary.todayTargetCalories ?? summary.targetCaloriesPerDay ?? 0).toLocaleString() }} kcal</span>
      </p>
      <div class="progress-bar">
        <div class="progress-bar__fill" :style="{ width: achievementRate + '%', backgroundColor: achievementColor }"></div>
      </div>
      <p class="rate-text">{{ achievementRate.toFixed(1) }}% 달성</p>
    </div>
          <div v-else class="placeholder">
            <p class="placeholder-text">앗! 아직 오늘 식단이 없어요. 🍱<br/>식단을 생성하면 칼로리가 슝~ 계산돼요! ✨</p>
          </div>  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  summary: Object,
});

const achievementRate = computed(() => {
  if (!props.summary) return 0;
  // If backend provides todayAchievementRate, use it
  if (props.summary.todayAchievementRate !== undefined && props.summary.todayAchievementRate !== null) {
      return props.summary.todayAchievementRate;
  }
  
  // Fallback calculation
  const current = props.summary.consumedCalories ?? 0;
  const target = props.summary.todayTargetCalories ?? props.summary.targetCaloriesPerDay ?? 1;
  return Math.min((current / target) * 100, 100);
});

const achievementColor = computed(() => {
  const rate = achievementRate.value;
  if (rate > 110) return '#ef4444'; // red (overeating)
  if (rate >= 80) return '#22c55e'; // green (good)
  return '#3b82f6'; // blue (in progress)
});
</script>

<style scoped>
.summary-card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
h3 {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
  color: #4b5563;
}
.calories {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}
.unit {
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
}
.progress-bar {
  width: 100%;
  height: 10px;
  background-color: #f3f4f6;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 8px;
}
.progress-bar__fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease, background-color 0.5s ease;
}
.rate-text {
  font-size: 14px;
  color: #6b7280;
  text-align: right;
  margin: 0;
}
.placeholder {
  color: #9ca3af;
  text-align: center;
  padding: 10px 0;
}
.placeholder-text {
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
}
</style>