<template>
  <nav class="sidebar-cards">
    <button 
      v-for="item in menuItems" 
      :key="item.path"
      class="nav-card"
      :class="{ active: currentPath === item.path }"
      @click="navigate(item.path)"
    >
      <div class="card-content">
        <span class="icon">{{ item.icon }}</span>
        <span class="label">{{ item.name }}</span>
      </div>
      <span class="chevron">❯</span>
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const currentPath = computed(() => route.path);

const menuItems = [
  { name: '대시보드', path: '/dashboard', icon: '🏠' },
  { name: '식단 관리', path: '/meal-plans', icon: '🍱' },
  { name: '쇼핑하기', path: '/shopping', icon: '🛒' },
  { name: '체중 기록', path: '/weights', icon: '⚖️' },
  { name: '프로필 설정', path: '/profile/edit', icon: '⚙️' },
];

function navigate(path) {
  router.push(path);
}
</script>

<style scoped>
.sidebar-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nav-card {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border: none;
  border-radius: 16px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
  color: #374151;
}

.nav-card:hover {
  transform: translateX(5px);
  background: #f0faf5;
}

.nav-card.active {
  background: #E8F5E9;
  border-left: 4px solid #4CAF50;
  color: #166534;
}

.card-content {
  display: flex;
  align-items: center;
}

.icon { 
  font-size: 1.2rem; 
  margin-right: 12px; 
}

.label { 
  font-weight: 600; 
  font-size: 15px;
}

.chevron { 
  color: #D1D5DB; 
  font-size: 12px; 
}
</style>
