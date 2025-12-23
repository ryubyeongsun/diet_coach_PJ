<template>
  <div class="card">
    <div class="card-header">
      <h2>오늘의 추천 행동</h2>
    </div>
    <div v-if="systemTodos.length > 0" class="todo-list">
      <div
        v-for="item in systemTodos"
        :key="item.id"
        class="todo-item"
        @click="goTo(item.path)"
      >
        <span class="icon">{{ item.icon }}</span>
        <div class="text-content">
          <span class="title">{{ item.title }}</span>
          <span class="subtitle">{{ item.subtitle }}</span>
        </div>
        <span class="arrow">&rarr;</span>
      </div>
    </div>
    <div v-else class="all-done">
      <span class="icon">🎉</span>
      <p>오늘의 주요 활동을 모두 마쳤어요. 멋져요!</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  summary: Object,
});

const router = useRouter();

const systemTodos = computed(() => {
  const todos = [];
  
  // 1. 오늘 체중 기록 여부 확인
  if (!props.summary || !props.summary.todayWeightRecorded) {
    todos.push({
      id: 'record-weight',
      icon: '⚖️',
      title: '오늘 체중 기록하기',
      subtitle: '변화를 꾸준히 추적해 보세요.',
      path: '/weights',
    });
  }

  // 2. 식단 생성 여부 확인
  if (props.summary && props.summary.mealPlanId) {
    todos.push({
      id: 'check-shopping-list',
      icon: '🛒',
      title: '이번 주 장보기',
      subtitle: '생성된 식단에 필요한 재료를 확인하세요.',
      path: `/shopping?planId=${props.summary.mealPlanId}`,
    });
  } else {
    todos.push({
      id: 'create-meal-plan',
      icon: '🍱',
      title: '월간 식단 생성하기',
      subtitle: '새로운 한 달을 계획해 보세요.',
      path: '/meal-plans',
    });
  }

  return todos;
});

const goTo = (path) => {
  if (path) {
    router.push(path);
  }
};
</script>

<style scoped>
.card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}
.todo-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.todo-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 10px;
  background-color: #f9fafb;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.todo-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
.icon {
  font-size: 24px;
}
.arrow {
  font-size: 20px;
  color: #9ca3af;
}
.text-content .title {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}
.text-content .subtitle {
  font-size: 13px;
  color: #6b7280;
  display: block;
}
.all-done {
  text-align: center;
  padding: 20px 0;
  color: #4b5563;
}
.all-done .icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}
</style>
