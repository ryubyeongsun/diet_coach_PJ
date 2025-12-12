<script setup>
import { RouterView, useRouter } from 'vue-router';

const router = useRouter();

const go = (path) => {
  router.push(path);
};
</script>

<template>
  <div class="layout">
    <!-- 상단 헤더 -->
    <header class="layout__header">
      <div class="layout__logo" @click="go('/')">
        🥑 <span>남남코치</span>
      </div>
      <div class="layout__header-right">
        <button class="layout__chip">30일 플랜</button>
        <button class="layout__chip layout__chip--primary">오늘 기록하기</button>
      </div>
    </header>

    <div class="layout__body">
      <!-- 왼쪽 사이드바 -->
      <aside class="layout__sidebar">
        <nav class="sidebar-nav">
          <div class="sidebar-nav__section">메뉴</div>

          <button class="sidebar-nav__item" @click="go('/meal-plans')">
            🍱 식단 관리
          </button>
          <button class="sidebar-nav__item" @click="go('/shopping')">
            🛒 재료 쇼핑
          </button>
          <button class="sidebar-nav__item" @click="go('/weights')">
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
</template>

<style scoped>
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
  gap: 8px;
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
  color: #374151;
  cursor: pointer;
}

.sidebar-nav__item:disabled {
  opacity: 0.5;
  cursor: default;
}

.sidebar-nav__item:not(:disabled):hover {
  background: #ecfdf5;
  color: #047857;
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
