<template>
  <div class="card summary-card">
    <h3>오늘 섭취 칼로리</h3>
    <div v-if="summary">
      <p class="calories">
        {{ summary.todayCalories?.toLocaleString() || 0 }}
        <span class="unit">/ {{ summary.targetCalories?.toLocaleString() || 0 }} kcal</span>
      </p>
      <div class="progress-bar">
        <div class="progress-bar__fill" :style="{ width: achievementRate + '%', backgroundColor: achievementColor }"></div>
      </div>
    </div>
    <div v-else class="placeholder">
      <p>요약 정보 없음</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  summary: Object,
});

const achievementRate = computed(() => {
  if (!props.summary || !props.summary.targetCalories) return 0;
  return Math.min((props.summary.todayCalories / props.summary.targetCalories) * 100, 100);
});

const achievementColor = computed(() => {
  if (!props.summary) return '#e5e7eb';
  const rate = props.summary.achievementRate;
  if (rate > 110) return '#ef4444'; // red
  if (rate >= 80) return '#22c55e'; // green
  return '#3b82f6'; // blue
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
  margin: 0 0 12px;
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
}
.progress-bar__fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease, background-color 0.5s ease;
}
.placeholder {
  color: #9ca3af;
}
</style>
