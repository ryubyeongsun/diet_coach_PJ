<template>
  <div class="card main-character-card">
    <div v-if="latestWeight > 0" class="character-content">
      <div class="character-visual">
        <!-- 3D Character Component -->
        <div class="character-canvas-wrapper">
          <Character3D :level="level" />
        </div>
        <div class="character-level">LV.{{ level }}</div>
      </div>
      <div class="character-feedback">
        <p class="speech-bubble">{{ statusText }}</p>
        <p class="key-metric">목표 체중 달성률: <strong>{{ achievementRate }}%</strong></p>
      </div>
    </div>
    <div v-else class="empty-state">
      <div class="character-avatar" :class="`level-3`"></div>
      <h2>남남코치에 오신 것을 환영합니다!</h2>
      <p>오늘 체중을 기록하고<br/>캐릭터와 함께 건강한 여정을 시작해보세요.</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { getCurrentUser } from '../../utils/auth';
import { calculateBmi, getBmiCategory, getCharacterLevel } from '../../utils/bmi';
import Character3D from './Character3D.vue';

const props = defineProps({
  summary: Object,
  trend: Object,
  latestWeight: Number,
});

const bmi = computed(() => 
  calculateBmi(props.latestWeight, getCurrentUser()?.height)
);

const category = computed(() => getBmiCategory(bmi.value));

const level = computed(() => {
  if (props.summary?.characterLevel) {
    return props.summary.characterLevel;
  }
  if (bmi.value > 0) {
    return getCharacterLevel(bmi.value);
  }
  return 3; // Default level
});

const achievementRate = computed(() => {
  const user = getCurrentUser();
  if (!user || !user.targetWeight) return 0;

  const current = props.latestWeight;
  const target = user.targetWeight;
  
  // Goal reached check
  if (user.goalType === 'LOSE_WEIGHT' && current <= target) return 100;
  if (user.goalType === 'GAIN_WEIGHT' && current >= target) return 100;
  if (user.goalType === 'MAINTAIN') {
      // For maintenance, maybe just 100 if within range?
      return Math.abs(current - target) < 1 ? 100 : 90;
  }

  // Proximity-based achievement rate
  let progress = 0;
  
  if (user.goalType === 'LOSE_WEIGHT') {
      // e.g. Current 80, Target 70 -> 70/80 = 87.5%
      progress = (target / current) * 100;
  } else if (user.goalType === 'GAIN_WEIGHT') {
      // e.g. Current 60, Target 70 -> 60/70 = 85.7%
      progress = (current / target) * 100;
  } else {
      // MAINTAIN
      // e.g. Target 70. Current 77 (10% diff) -> 90% achieved.
      // diff ratio = abs(77-70)/70 = 0.1
      const diffRatio = Math.abs(current - target) / target;
      progress = Math.max(0, (1 - diffRatio) * 100);
  }

  return Math.max(0, Math.min(100, progress)).toFixed(0);
});

const weightChangeLast7Days = computed(() => {
  if (!props.trend?.dayTrends || props.trend.dayTrends.length < 2) return 0;
  
  const weightDays = props.trend.dayTrends.filter(d => d.weight !== null).sort((a, b) => new Date(a.date) - new Date(b.date));
  if (weightDays.length < 2) return 0;

  const recentDays = weightDays.slice(-7);
  if (recentDays.length < 2) return 0;
  
  return recentDays[recentDays.length - 1].weight - recentDays[0].weight;
});

const statusText = computed(() => {
  if (weightChangeLast7Days.value < -0.5) {
    return "최근 체중 감량 추세가 훌륭해요!";
  }
  if (weightChangeLast7Days.value > 0.5) {
    return "체중이 조금 늘었어요. 다시 힘내봐요!";
  }
  if (props.summary?.achievementRate > 110) {
    return "목표 칼로리를 조금 초과했지만 괜찮아요!";
  }
  if (props.summary?.achievementRate >= 80) {
    return "목표에 거의 도달했어요. 정말 잘 하고 있어요!";
  }
  return "꾸준함이 중요해요. 오늘도 화이팅!";
});
</script>

<style scoped>
.main-character-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
  min-height: 400px;
  background: transparent; /* Changed to transparent */
  /* border-radius: 12px; Removed */
  /* box-shadow: 0 8px 25px rgba(0,0,0,0.1); Removed */
  position: relative;
  /* overflow: hidden; Removed to let elements breathe */
}
.character-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
  width: 100%;
}
.character-visual {
  position: relative;
  margin-bottom: 40px; /* Increased margin for separation */
  width: 100%;
  display: flex;
  justify-content: center;
}
.character-canvas-wrapper {
  width: 360px; /* Increased size for prominence */
  height: 360px;
  position: relative;
  /* Removed border and box-shadow for a cleaner look */
}
/* Removed old avatar styles */

.character-level {
  position: absolute;
  bottom: -20px; /* Lowered position */
  left: 50%;
  transform: translateX(-50%);
  background-color: #22c55e;
  color: white;
  border-radius: 999px;
  padding: 8px 24px; /* Slightly wider padding */
  font-size: 20px; /* Increased font size */
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
  white-space: nowrap;
  z-index: 3;
}
.character-feedback {
  margin-top: 20px;
}
.speech-bubble {
  position: relative;
  background: #f0f0f0; /* Light gray for bubble */
  border-radius: 10px;
  padding: 12px 20px;
  margin: 0 auto 15px auto;
  max-width: 80%;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.speech-bubble::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 20px;
  height: 20px;
  background: #f0f0f0;
  border-radius: 3px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: -1;
}
.key-metric {
  font-size: 20px;
  color: #6b7280;
  margin: 0;
}
.key-metric strong {
  font-size: 32px;
  font-weight: 800;
  color: #1f2937;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
}
.empty-state .character-avatar {
  width: 100px;
  height: 100px;
  font-size: 50px;
  margin-bottom: 20px;
  background-color: #a0d9b4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-state h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 10px;
  color: #1f2937;
}
.empty-state p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}
</style>
