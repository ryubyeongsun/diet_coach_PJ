<template>
  <div class="bmi-gauge-container">
    <div class="gauge-wrapper">
      <div 
        v-for="level in levels" 
        :key="level.level"
        class="gauge-segment"
        :class="{
          'active': currentLevel === level.level,
          [`level-${level.level}`]: true
        }"
      >
        <div class="segment-label">
          <span class="level-text">LV.{{ level.level }}</span>
          <span class="status-text">{{ level.status }}</span>
        </div>
        <div class="segment-bar">
          <div v-if="currentLevel === level.level" class="current-indicator">
            <div class="indicator-dot"></div>
            <div class="indicator-line"></div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="bmi-value">
      <span class="value">{{ bmi.toFixed(1) }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  bmi: {
    type: Number,
    required: true
  },
  level: {
    type: Number,
    required: true,
    default: 3
  }
});

// BMI 레벨 구간 정의 (한국/아시아태평양 기준)
// LV1: < 18.5 (마름)
// LV2: 18.5 ~ 23 (정상)
// LV3: 23 ~ 25 (과체중)
// LV4: 25 ~ 30 (비만)
// LV5: >= 30 (고도비만)
const levels = [
  { level: 1, status: '마름', minBmi: 0, maxBmi: 18.5 },
  { level: 2, status: '정상', minBmi: 18.5, maxBmi: 23.0 },
  { level: 3, status: '과체중', minBmi: 23.0, maxBmi: 25.0 },
  { level: 4, status: '비만', minBmi: 25.0, maxBmi: 30.0 },
  { level: 5, status: '고도비만', minBmi: 30.0, maxBmi: 100 }
];

const currentLevel = computed(() => props.level);
</script>

<style scoped>
.bmi-gauge-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  width: 160px; /* 고정 너비로 레이아웃 안정화 */
}

.gauge-wrapper {
  display: flex;
  flex-direction: column-reverse; /* LV.1이 아래로, LV.5가 위로 */
  gap: 2px;
  background: white;
  padding: 16px;
  border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

.gauge-segment {
  display: flex;
  align-items: center;
  gap: 8px; /* 간격 줄임 */
  padding: 10px 8px;
  border-radius: 12px;
  transition: all 0.3s ease;
  position: relative;
  opacity: 0.5;
}

.gauge-segment.active {
  opacity: 1;
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 1;
  background: white; /* 배경색 명시 */
}

/* LV.1 저체중 - 파란색 */
.gauge-segment.level-1 {
  background: linear-gradient(90deg, rgba(66, 165, 245, 0.1) 0%, rgba(66, 165, 245, 0.2) 100%);
  border-left: 4px solid #42A5F5;
}
.gauge-segment.level-1.active {
  background: linear-gradient(90deg, rgba(66, 165, 245, 0.2) 0%, rgba(66, 165, 245, 0.3) 100%);
}

/* LV.2 정상 - 초록색 */
.gauge-segment.level-2 {
  background: linear-gradient(90deg, rgba(102, 187, 106, 0.1) 0%, rgba(102, 187, 106, 0.2) 100%);
  border-left: 4px solid #66BB6A;
}
.gauge-segment.level-2.active {
  background: linear-gradient(90deg, rgba(102, 187, 106, 0.2) 0%, rgba(102, 187, 106, 0.3) 100%);
}

/* LV.3 과체중 - 노란색 */
.gauge-segment.level-3 {
  background: linear-gradient(90deg, rgba(255, 193, 7, 0.1) 0%, rgba(255, 193, 7, 0.2) 100%);
  border-left: 4px solid #FFC107;
}
.gauge-segment.level-3.active {
  background: linear-gradient(90deg, rgba(255, 193, 7, 0.2) 0%, rgba(255, 193, 7, 0.3) 100%);
}

/* LV.4 비만 - 주황색 */
.gauge-segment.level-4 {
  background: linear-gradient(90deg, rgba(255, 152, 0, 0.1) 0%, rgba(255, 152, 0, 0.2) 100%);
  border-left: 4px solid #FF9800;
}
.gauge-segment.level-4.active {
  background: linear-gradient(90deg, rgba(255, 152, 0, 0.2) 0%, rgba(255, 152, 0, 0.3) 100%);
}

/* LV.5 고도비만 - 빨간색 */
.gauge-segment.level-5 {
  background: linear-gradient(90deg, rgba(239, 83, 80, 0.1) 0%, rgba(239, 83, 80, 0.2) 100%);
  border-left: 4px solid #EF5350;
}
.gauge-segment.level-5.active {
  background: linear-gradient(90deg, rgba(239, 83, 80, 0.2) 0%, rgba(239, 83, 80, 0.3) 100%);
}

.segment-label {
  display: flex;
  flex-direction: column;
  min-width: 60px;
}

.level-text {
  font-size: 13px;
  font-weight: 700;
  color: #424242;
  margin-bottom: 2px;
}

.status-text {
  font-size: 11px;
  font-weight: 600;
  color: #757575;
}

.segment-bar {
  flex: 1;
  height: 32px;
  /* background: rgba(0,0,0,0.05);  삭제 - 배경이 겹쳐서 지저분해 보일 수 있음 */
  position: relative;
  overflow: visible;
}

.current-indicator {
  position: absolute;
  right: -8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
}

.indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2E7D32;
  box-shadow: 0 0 0 4px rgba(46, 125, 50, 0.2);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 4px rgba(46, 125, 50, 0.2);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 0 8px rgba(46, 125, 50, 0.1);
  }
}

.indicator-line {
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 6px solid #2E7D32;
}

.bmi-value {
  background: white;
  padding: 8px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  text-align: center;
  width: 100%;
  box-sizing: border-box;
}

.bmi-value .value {
  font-size: 18px;
  font-weight: 700;
  color: #2E7D32;
}

.bmi-value .value::after {
  content: ' BMI';
  font-size: 11px;
  color: #7CB342;
  font-weight: 600;
  margin-left: 2px;
}
</style>
