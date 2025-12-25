<template>
  <section class="avatar-section">
    <!-- 배경 부유 도형 -->
    <div class="floating-shape shape-1"></div>
    <div class="floating-shape shape-2"></div>

    <!-- 메인 컨텐츠 영역 (좌: 게이지 / 우: 캐릭터) -->
    <div class="avatar-content-row">
      <!-- 1. 좌측: BMI 게이지 -->
      <div class="gauge-area">
        <BmiGauge :bmi="bmi" :level="level" />
      </div>

      <!-- 2. 중앙: 캐릭터 및 정보 -->
      <div class="character-center-area">
        <!-- 말풍선 영역 -->
        <div class="speech-bubble-container">
          <div class="bubble bubble-left" v-html="coachMessage"></div>
          <div class="bubble bubble-right">
            <template v-if="goalRemaining > 0">목표까지 {{ goalRemaining.toFixed(1) }}kg!</template>
            <template v-else>목표 달성! 🎉</template>
          </div>
        </div>

        <!-- 캐릭터 영역 -->
        <div class="character-wrapper">
          <div class="canvas-placeholder">
            <Character3D :level="level" />
          </div>
          <div class="character-shadow"></div>
        </div>

        <!-- 상태 배지 -->
        <div class="status-badge">LV.{{ level }}</div>
        
        <!-- 달성률 텍스트 -->
        <div class="achievement-text">
          목표 체중 달성률: <strong>{{ achievementRate }}%</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import Character3D from './Character3D.vue';
import BmiGauge from './BmiGauge.vue';
import { getCurrentUser } from '../../utils/auth';
import { calculateBmi, getCharacterLevel } from '../../utils/bmi';

const props = defineProps({
  summary: Object,
  trend: Object,
  latestWeight: Number,
});

// Calculate BMI and Level
const bmi = computed(() => 
  calculateBmi(props.latestWeight, getCurrentUser()?.height)
);

const level = computed(() => {
  if (props.summary?.characterLevel) {
    return props.summary.characterLevel;
  }
  if (bmi.value > 0) {
    return getCharacterLevel(bmi.value);
  }
  return 3;
});

// Calculate Achievement Rate
const achievementRate = computed(() => {
  const user = getCurrentUser();
  if (!user || !user.targetWeight) return 0;

  const current = props.latestWeight;
  const target = user.targetWeight;
  
  if (user.goalType === 'LOSE_WEIGHT' && current <= target) return 100;
  if (user.goalType === 'GAIN_WEIGHT' && current >= target) return 100;
  if (user.goalType === 'MAINTAIN') {
      return Math.abs(current - target) < 1 ? 100 : 90;
  }

  let progress = 0;
  if (user.goalType === 'LOSE_WEIGHT') {
      progress = (target / current) * 100; // Simplified for MVP
  } else if (user.goalType === 'GAIN_WEIGHT') {
      progress = (current / target) * 100;
  } else {
      const diffRatio = Math.abs(current - target) / target;
      progress = Math.max(0, (1 - diffRatio) * 100);
  }

  return Math.max(0, Math.min(100, progress)).toFixed(0);
});

// Calculate Remaining Weight
const goalRemaining = computed(() => {
    const user = getCurrentUser();
    if (!user || !user.targetWeight) return 0;
    return Math.abs(props.latestWeight - user.targetWeight);
});

// Determine Coach Message
const coachMessage = computed(() => {
  // Use recent weight trend if available
  if (props.trend?.dayTrends && props.trend.dayTrends.length >= 2) {
      const sorted = [...props.trend.dayTrends].filter(d => d.weight).sort((a,b) => new Date(a.date) - new Date(b.date));
      if (sorted.length >= 2) {
          const diff = sorted[sorted.length - 1].weight - sorted[0].weight;
          if (diff < -0.5) return "체중이 잘 빠지고 있어요!<br/>계속 이렇게 가봐요! 🔥";
          if (diff > 0.5) return "체중이 조금 늘었어요.<br/>다시 힘내봐요! 💪";
      }
  }
  
  // Use today's calorie achievement
  if (props.summary) {
      const rate = props.summary.todayAchievementRate || 0;
      if (rate > 100) return "오늘 좀 많이 드셨나요?<br/>운동으로 태워봐요! 🏃‍♂️";
      if (rate > 80) return "오늘 목표 달성 직전이에요!<br/>조금만 더 힘내요 ✨";
  }

  return "건강한 하루 되세요!<br/>오늘도 응원할게요 🥑";
});

</script>

<style scoped>
.avatar-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 700px;
  padding-top: 40px; /* 패딩 조정 */
}

/* 레이아웃 래퍼 */
.avatar-content-row {
  display: flex;
  align-items: center; /* 수직 중앙 정렬 */
  justify-content: center;
  gap: 40px; /* 게이지와 캐릭터 사이 간격 */
  width: 100%;
  max-width: 1000px;
  z-index: 5;
}

.gauge-area {
  flex-shrink: 0; /* 크기 줄어들지 않도록 */
  margin-top: 60px; /* 캐릭터와 시각적 밸런스를 맞추기 위해 약간 내림 */
}

.character-center-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

/* 말풍선 스타일 */
.bubble {
  position: absolute;
  background: white;
  padding: 20px 32px;
  border-radius: 30px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.12);
  font-size: 18px;
  font-weight: 700;
  color: #2E7D32;
  z-index: 10;
  line-height: 1.4;
  white-space: nowrap;
  animation: float-bubble 4s ease-in-out infinite;
}
.bubble-left { 
    top: 5%; 
    left: -15%; /* 왼쪽으로 더 이동 */
    animation-delay: 0s;
}
.bubble-right { 
    top: 10%; 
    right: -15%; /* 오른쪽으로 더 이동 */
    animation-delay: 2s;
}

@keyframes float-bubble {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-20px); }
}

/* 캐릭터 래퍼 및 그림자 */
.character-wrapper {
    position: relative;
    z-index: 5;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 0;
}

.canvas-placeholder {
    width: 750px; /* 초대형 사이즈로 확대 */
    height: 750px;
}

.character-shadow {
  width: 300px; /* 그림자 대폭 확대 */
  height: 40px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: 50%;
  filter: blur(12px);
  margin-top: -80px;
  animation: shadow-pulse 6s ease-in-out infinite;
}

@keyframes shadow-pulse {
    0%, 100% { transform: scale(1.2); opacity: 0.8; }
    50% { transform: scale(0.8); opacity: 0.3; }
}

/* 상태 배지 */
.status-badge {
    margin-top: 20px;
    background: #22c55e;
    color: white;
    font-size: 24px;
    font-weight: 800;
    padding: 10px 40px;
    border-radius: 99px;
    box-shadow: 0 8px 20px rgba(34, 197, 94, 0.4);
    z-index: 10;
}

.achievement-text {
    margin-top: 20px;
    font-size: 20px;
    color: #4b5563;
}
.achievement-text strong {
    font-size: 28px;
    color: #111827;
}

/* 배경 도형 애니메이션 */
.floating-shape {
  position: absolute;
  background: rgba(255, 255, 255, 0.6);
  transform: rotate(45deg);
  border-radius: 50px;
  z-index: 0;
  box-shadow: 0 10px 40px rgba(0,0,0,0.02);
}
.shape-1 { 
    width: 200px; 
    height: 200px; 
    top: 0%; 
    left: -10%; 
    animation: float 8s infinite ease-in-out; 
}
.shape-2 {
    width: 150px;
    height: 150px;
    bottom: 10%;
    right: -5%;
    animation: float 7s infinite ease-in-out reverse;
    background: rgba(232, 245, 233, 0.8);
}

@keyframes float { 
    0%, 100% { transform: rotate(45deg) translateY(0); } 
    50% { transform: rotate(45deg) translateY(-20px); } 
}
</style>
