<template>
  <div>
    <h2 class="section-title">이번 주 요약</h2>
    <div class="summary-grid">
      <div class="card">
        <h3>평균 섭취 칼로리</h3>
        <p>{{ weeklySummary.avgIntake?.toLocaleString() || 0 }} <span class="unit">kcal</span></p>
      </div>
      <div class="card">
        <h3>체중 변화</h3>
        <p>
          <span v-if="weeklySummary.weightChange > 0" class="positive">
            +{{ weeklySummary.weightChange.toFixed(1) }} kg
          </span>
          <span v-else-if="weeklySummary.weightChange < 0" class="negative">
            {{ weeklySummary.weightChange.toFixed(1) }} kg
          </span>
          <span v-else>0 kg</span>
        </p>
      </div>
      <div class="card">
        <h3>목표 달성일</h3>
        <p>{{ weeklySummary.goalAchievementDays }} <span class="unit">일</span></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  trend: Object,
});

const weeklySummary = computed(() => {
  if (!props.trend?.days || props.trend.days.length === 0) {
    return {
      avgIntake: 0,
      weightChange: 0,
      goalAchievementDays: 0,
    };
  }

  const days = props.trend.days;

  // 1. 평균 섭취 칼로리
  const intakeDays = days.filter(d => d.totalCalories > 0);
  const avgIntake = intakeDays.length > 0 
    ? intakeDays.reduce((sum, day) => sum + day.totalCalories, 0) / intakeDays.length
    : 0;

  // 2. 체중 변화
  const weightDays = days.filter(d => d.weight !== null).sort((a, b) => new Date(a.date) - new Date(b.date));
  const firstWeight = weightDays[0]?.weight;
  const lastWeight = weightDays[weightDays.length - 1]?.weight;
  const weightChange = (weightDays.length >= 2 && firstWeight && lastWeight) ? lastWeight - firstWeight : 0;

  // 3. 목표 달성 일수 (달성률 80-110% 기준)
  const goalAchievementDays = days.reduce((count, day) => {
    if (day.achievementRate >= 80 && day.achievementRate <= 110) {
      return count + 1;
    }
    return count;
  }, 0);
  
  return {
    avgIntake: Math.round(avgIntake),
    weightChange,
    goalAchievementDays,
  };
});
</script>

<style scoped>
.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.card h3 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: #4b5563;
}
.card p {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}
.card p .unit {
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  margin-left: 4px;
}
.positive {
  color: #ef4444; /* 체중 증가는 빨간색 */
}
.negative {
  color: #22c55e; /* 체중 감소는 녹색 */
}
</style>
