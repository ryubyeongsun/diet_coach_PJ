<template>
  <div class="loading-overlay">
    <div class="loading-content">
      <div class="character-bounce">
        <!-- 아이콘 (이모지) -->
        {{ icon }}
      </div>
      <h3 class="loading-title">{{ title }}</h3>
      <p class="loading-message">{{ currentMessage }}</p>
      <div class="progress-bar">
        <div class="progress-fill"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, defineProps } from "vue";

const props = defineProps({
  title: {
    type: String,
    default: "아보핏 도와드리고 있어요!",
  },
  messages: {
    type: Array,
    default: () => [
      "사용자님의 건강 데이터를 분석하고 있습니다...",
      "이번 달 예산에 맞는 재료를 찾는 중이에요...",
      "영양 밸런스를 꼼꼼히 체크하고 있습니다...",
      "맛있는 레시피를 조합하는 중입니다...",
      "거의 다 됐어요! 잠시만 기다려주세요...",
    ],
  },
  icon: {
    type: String,
    default: "🧑‍🍳",
  },
});

const currentMessage = ref(props.messages[0]);
let intervalId = null;

onMounted(() => {
  let index = 0;
  intervalId = setInterval(() => {
    index = (index + 1) % props.messages.length;
    currentMessage.value = props.messages[index];
  }, 2500); // 2.5초마다 메시지 변경 (조금 더 빠르게)
});

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId);
});
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.95);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-content {
  text-align: center;
  max-width: 400px;
  padding: 20px;
}

.character-bounce {
  font-size: 80px;
  margin-bottom: 20px;
  animation: bounce 1s infinite alternate;
}

@keyframes bounce {
  from { transform: translateY(0); }
  to { transform: translateY(-20px); }
}

.loading-title {
  font-size: 24px;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 12px;
}

.loading-message {
  font-size: 16px;
  color: #6b7280;
  margin-bottom: 30px;
  min-height: 24px; /* 텍스트 변경 시 레이아웃 덜컹거림 방지 */
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 30%;
  background-color: #3b82f6;
  border-radius: 999px;
  animation: progress 2s infinite ease-in-out;
}

@keyframes progress {
  0% { left: -30%; width: 30%; }
  50% { width: 60%; }
  100% { left: 100%; width: 30%; }
}
</style>
