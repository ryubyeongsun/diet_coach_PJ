<template>
  <div class="landing-page">
    <header class="landing-header">
      <div class="header-content">
        <h1 class="logo">남남코치</h1>
        <div class="cta-group">
          <router-link to="/login" class="btn btn-secondary">로그인</router-link>
          <router-link to="/signup" class="btn btn-primary">회원가입</router-link>
        </div>
      </div>
    </header>

    <main class="landing-main">
      <section class="hero-section">
        <h2 class="hero-title">
          예산·칼로리·체중을<br />
          한 번에 관리하는 식단 코치
        </h2>
        <p class="hero-desc">
          한 달 예산 안에서 자동으로 식단을 생성하고<br />
          체중 변화까지 함께 관리하는 서비스
        </p>
        <div class="hero-cta">
          <router-link to="/signup" class="btn btn-primary btn-lg">지금 시작하기</router-link>
        </div>
      </section>

      <section class="feature-section">
        <div class="feature-card">
          <div class="icon">📅</div>
          <h3>식단 자동 생성</h3>
          <p>한 달 예산과 목표 칼로리에 맞춰<br />최적의 식단을 자동으로 짜드립니다.</p>
        </div>
        <div class="feature-card">
          <div class="icon">⚖️</div>
          <h3>체중 관리</h3>
          <p>매일 체중을 기록하고<br />변화를 시각적으로 확인하세요.</p>
        </div>
        <div class="feature-card">
          <div class="icon">🛒</div>
          <h3>장보기 리스트</h3>
          <p>식단에 필요한 재료를<br />자동으로 리스트업 해드립니다.</p>
        </div>
        <div class="feature-card">
          <div class="icon">🐣</div>
          <h3>캐릭터 성장</h3>
          <p>목표를 달성하며<br />나만의 캐릭터를 키워보세요.</p>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <p>© 2025 Diet Coach Project. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
// Logic if needed
</script>

<style scoped>
/* Landing Page Scoped Styles */
.landing-page {
  font-family: 'Pretendard', sans-serif;
  color: #333;
  background-color: #f9fafb;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.landing-header {
  background-color: #fff;
  border-bottom: 1px solid #eee;
  padding: 16px 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 24px;
  font-weight: 800;
  color: #3b82f6;
  margin: 0;
}

.cta-group {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  font-size: 14px;
}

.btn-primary {
  background-color: #3b82f6;
  color: #fff;
  border: 1px solid #3b82f6;
}

.btn-primary:hover {
  background-color: #2563eb;
}

.btn-secondary {
  background-color: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background-color: #f3f4f6;
}

.btn-lg {
  padding: 12px 32px;
  font-size: 18px;
}

.landing-main {
  flex: 1;
}

.hero-section {
  text-align: center;
  padding: 80px 24px;
  background: linear-gradient(180deg, #fff 0%, #f9fafb 100%);
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 24px;
  color: #111827;
}

.hero-desc {
  font-size: 20px;
  color: #6b7280;
  margin-bottom: 40px;
  line-height: 1.6;
}

.feature-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 24px 100px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 32px;
}

.feature-card {
  background: #fff;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.2s;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-card .icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #1f2937;
}

.feature-card p {
  color: #6b7280;
  line-height: 1.5;
}

.landing-footer {
  text-align: center;
  padding: 32px;
  border-top: 1px solid #eee;
  color: #9ca3af;
  font-size: 14px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }
  .hero-desc {
    font-size: 16px;
  }
  .feature-section {
    grid-template-columns: 1fr;
  }
}
</style>
