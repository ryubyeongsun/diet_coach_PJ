<script setup>
import { RouterView, useRouter, useRoute } from "vue-router";
import { ref, computed, watch } from "vue";
import { getCurrentUser, clearAuth } from "./utils/auth";
import { globalState, setWeightModalOpen } from "./utils/globalState";
import WeightRecordModal from "./components/common/WeightRecordModal.vue";

const router = useRouter();
const route = useRoute();

const currentUser = ref(getCurrentUser());

const isAuthPage = computed(
  () => route.path === "/login" || route.path === "/signup",
);

const cartItemCount = computed(() => {
  return globalState.cart.reduce((total, item) => total + item.quantity, 0);
});

watch(
  () => route.path,
  () => {
    currentUser.value = getCurrentUser();
  },
);

const go = (path) => {
  router.push(path);
};

const handleLogout = () => {
  clearAuth();
  currentUser.value = null;
  router.push("/login");
};

// 모달 저장 후 데이터 갱신을 위한 핸들러
// 페이지를 새로고침하여 대시보드 및 관련 데이터를 업데이트합니다.
const handleWeightSaved = () => {
  setWeightModalOpen(false);
  // 현재 라우트가 대시보드나 체중 페이지일 경우에만 새로고침
  if (route.path.startsWith('/dashboard') || route.path.startsWith('/weights') || route.path === '/') {
    window.location.reload();
  }
};
</script>

<template>
  <!-- 전역 UI -->
  <WeightRecordModal
    :is-open="globalState.isWeightModalOpen"
    @close="setWeightModalOpen(false)"
    @saved="handleWeightSaved"
  />
  <div class="global-loader" v-if="globalState.isLoading">
    <div class="spinner"></div>
  </div>
  <div class="global-error" v-if="globalState.error">
    {{ globalState.error }}
  </div>

  <div v-if="!isAuthPage" class="layout">
    <!-- 상단 헤더 -->
    <header class="layout__header">
      <div class="layout__logo" @click="go('/')">🥑 <span>남남코치</span></div>
      <div class="layout__header-right">
        <div v-if="currentUser" class="user-info">
          <button class="layout__chip cta-btn" @click="setWeightModalOpen(true)">+ 오늘 체중 기록</button>
          <button class="cta-btn cta-btn--primary" @click="go('/meal-plans')">+ 식단 생성</button>
          <span class="divider"></span>
          <span class="cart-status" @click="go('/cart')"
            >🛒 ({{ cartItemCount }})</span
          >
          <span>{{ currentUser.name }}님</span>
          <button
            @click="handleLogout"
            class="layout__chip layout__chip--secondary"
          >
            로그아웃
          </button>
        </div>
        <div v-else>
          <button class="layout__chip" @click="go('/login')">로그인</button>
        </div>
      </div>
    </header>

    <div class="layout__body">
      <!-- 왼쪽 사이드바 -->
      <aside class="layout__sidebar">
        <nav class="sidebar-nav">
          <div class="sidebar-nav__section">메뉴</div>

          <button
            class="sidebar-nav__item"
            :class="{
              'sidebar-nav__item--active':
                route.path === '/' || route.path.startsWith('/dashboard'),
            }"
            @click="go('/dashboard')"
          >
            📊 대시보드
          </button>
          <button
            class="sidebar-nav__item"
            :class="{
              'sidebar-nav__item--active': route.path.startsWith('/meal-plans'),
            }"
            @click="go('/meal-plans')"
          >
            🍱 식단 관리
          </button>
          <button
            class="sidebar-nav__item"
            :class="{
              'sidebar-nav__item--active':
                route.path.startsWith('/shopping') ||
                route.path.startsWith('/cart'),
            }"
            @click="go('/shopping')"
          >
            🛒 재료 쇼핑
          </button>
          <button
            class="sidebar-nav__item"
            :class="{
              'sidebar-nav__item--active': route.path.startsWith('/weights'),
            }"
            @click="go('/weights')"
          >
            ⚖️ 체중 기록
          </button>
          <button class="sidebar-nav__item" disabled>
            💪 운동 기록 (준비중)
          </button>

          <div class="sidebar-nav__section sidebar-nav__section--sub">
            오늘의 식단
          </div>
          <div class="sidebar-meal">
            <div class="sidebar-meal__thumb">🍛</div>
            <div class="sidebar-meal__info">
              <div class="sidebar-meal__title">닭가슴살 샐러드</div>
              <div class="sidebar-meal__cal">420 kcal</div>
            </div>
          </div>
        </nav>
      </aside>

      <!-- 메인 컨텐츠 -->
      <main class="layout__main">
        <RouterView />
      </main>
    </div>
  </div>
  <div v-else>
    <RouterView />
  </div>
</template>

<style scoped>
.global-loader {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}
.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-left-color: #09f;
  animation: spin 1s ease infinite;
}
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.global-error {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #ef4444;
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 9999;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
}

.cart-status {
  font-weight: 700;
  cursor: pointer;
}

.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  /* 민트 계열 그라데이션 배경 */
  background: linear-gradient(135deg, #b9f3e6 0%, #e3f6ff 40%, #ffffff 100%);
}

/* 헤더 */
.layout__header {
  height: 56px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
}

.layout__logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
  font-size: 18px;
  color: #047857;
  cursor: pointer;
}

.layout__logo span {
  transform: translateY(-1px);
}

.layout__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cta-btn {
  font-weight: 600;
  font-size: 13px;
  background-color: #fff;
  border: 1px solid #d1d5db;
  border-radius: 999px; /* Moved from layout__chip */
  padding: 6px 12px; /* Moved from layout__chip */
  cursor: pointer; /* Moved from layout__chip */
  transition: all 0.2s ease-in-out;
  white-space: nowrap;
}
.cta-btn:hover {
  filter: brightness(0.95);
}

.cta-btn--primary {
  background-color: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.divider {
  height: 20px;
  width: 1px;
  background-color: #e5e7eb;
  margin: 0 8px;
}

.layout__chip {
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.6);
  background: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  padding: 6px 12px;
  cursor: pointer;
}

.layout__chip--primary {
  border-color: #22c55e;
  background: #22c55e;
  color: white;
}

.layout__chip--secondary {
  border-color: #ef4444;
  background: #fee2e2;
  color: #991b1b;
}

/* 바디 */
.layout__body {
  flex: 1;
  display: flex;
  padding: 18px 24px 24px;
  gap: 18px;
}

/* 사이드바 */
.layout__sidebar {
  width: 220px;
  min-width: 200px;
}

.sidebar-nav {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 18px;
  padding: 16px 14px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-nav__section {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 4px;
}

.sidebar-nav__section--sub {
  margin-top: 8px;
}

.sidebar-nav__item {
  width: 100%;
  text-align: left;
  border-radius: 999px;
  border: none;
  background: transparent;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}

.sidebar-nav__item:disabled {
  opacity: 0.5;
  cursor: default;
}

.sidebar-nav__item:not(:disabled):hover {
  background: #ecfdf5;
  color: #047857;
}

.sidebar-nav__item--active {
  background: #d1fae5;
  color: #065f46;
  font-weight: 700;
}

.sidebar-meal {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 12px;
  background: #f9fafb;
}

.sidebar-meal__thumb {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-meal__info {
  font-size: 12px;
}

.sidebar-meal__title {
  font-weight: 600;
  color: #111827;
}

.sidebar-meal__cal {
  color: #6b7280;
}

/* 메인 영역 */
.layout__main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
