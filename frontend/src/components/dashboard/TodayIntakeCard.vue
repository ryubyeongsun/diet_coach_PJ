<template>
  <div class="stat-card">
    <div class="card-header">오늘 섭취 칼로리</div>
    
    <div v-if="summary" class="content-wrapper">
      <div class="calorie-info">
        <span class="current">{{ (summary.consumedCalories ?? 0).toLocaleString() }}</span>
        <span class="total">/ {{ (summary.todayTargetCalories ?? summary.targetCaloriesPerDay ?? 0).toLocaleString() }} kcal</span>
      </div>
      
      <div class="progress-bar">
        <div 
          class="fill" 
          :style="{ width: Math.min(achievementRate, 100) + '%', backgroundColor: achievementColor }"
        ></div>
      </div>
      
      <div class="percentage">{{ achievementRate.toFixed(1) }}% 달성</div>
    </div>
    
    <div v-else class="placeholder">
      <p class="placeholder-text">식단이 생성되면<br/>칼로리가 표시됩니다 ✨</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  summary: Object,
});

const achievementRate = computed(() => {
  if (!props.summary) return 0;
  if (props.summary.todayAchievementRate !== undefined && props.summary.todayAchievementRate !== null) {
      return props.summary.todayAchievementRate;
  }
  const current = props.summary.consumedCalories ?? 0;
  const target = props.summary.todayTargetCalories ?? props.summary.targetCaloriesPerDay ?? 1;
  return Math.min((current / target) * 100, 100);
});

const achievementColor = computed(() => {
  const rate = achievementRate.value;
  if (rate > 110) return '#ef4444'; // Red for overeating
  if (rate >= 80) return '#22c55e'; // Green for good target
  return '#22C55E'; // Default green (PRD style)
});
</script>

<style scoped>
.stat-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
}

.card-header { 
  font-size: 14px; 
  color: #6B7280; 
  margin-bottom: 12px; 
  font-weight: 500;
}

.calorie-info { 
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.current { 
  font-size: 28px; 
  font-weight: 800; 
  color: #111827; 
  line-height: 1;
}

.total { 
  font-size: 14px; 
  color: #9CA3AF; 
  font-weight: 500;
}

.progress-bar {
  height: 8px;
  background: #E5E7EB;
  border-radius: 4px;
  margin-bottom: 8px;
  overflow: hidden;
  width: 100%;
}

.fill { 
  height: 100%; 
  background: #22C55E; 
  transition: width 1s ease-out;
  border-radius: 4px;
}

.percentage { 
  font-size: 13px; 
  color: #6B7280; 
  text-align: right; 
  font-weight: 500;
}

.placeholder {
  color: #9ca3af;
  text-align: center;
  padding: 20px 0;
}
.placeholder-text {
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
</style>