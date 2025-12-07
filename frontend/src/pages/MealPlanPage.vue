<template>
  <div class="page">
    <!-- 상단 헤더 -->
    <header class="page__header">
      <div>
        <h1>한 달 식단 플랜</h1>
        <p>
          TDEE와 예산을 기반으로 자동 생성될 식단의 레이아웃입니다.
          지금은 더미 데이터로 UI만 먼저 잡아두었습니다.
        </p>
      </div>
      <NnButton block>
        식단 자동 생성 (추후 연동)
      </NnButton>
    </header>

    <!-- 가운데: 히어로 + 통계 카드 영역 -->
    <section class="page__top-grid">
      <!-- 아보카도 히어로 카드 -->
      <NnCard class="hero-card">
        <div class="hero-card__inner">
          <div class="hero-card__character">
            <div class="hero-card__circle">
              🥑
            </div>
            <div class="hero-card__speech">
              <span>오늘도 </span><strong>80%</strong><span> 만큼 잘 하고 있어요!</span>
            </div>
          </div>

          <div class="hero-card__info">
            <div class="hero-card__row">
              <span class="hero-card__label">이번 주 평균 섭취</span>
              <span class="hero-card__value">1,820 kcal</span>
            </div>
            <div class="hero-card__row">
              <span class="hero-card__label">이번 주 평균 소모</span>
              <span class="hero-card__value">2,050 kcal</span>
            </div>
            <div class="hero-card__row hero-card__row--highlight">
              <span class="hero-card__label">이번 주 체중 변화</span>
              <span class="hero-card__value hero-card__value--good">-0.6 kg</span>
            </div>
          </div>
        </div>
      </NnCard>

      <!-- 오른쪽 통계 카드 2개 -->
      <div class="stat-column">
        <NnCard
          title="이번 달 목표 달성률"
          subtitle="목표 칼로리 대비 평균 섭취량 기준"
          class="stat-card"
        >
          <div class="stat-card__main">
            <div class="stat-card__percent">
              <span class="stat-card__percent-value">80%</span>
              <span class="stat-card__percent-label">달성 중</span>
            </div>
            <div class="stat-card__bars">
              <div class="stat-card__bar">
                <span>섭취 칼로리</span>
                <div class="stat-card__bar-track">
                  <div class="stat-card__bar-fill stat-card__bar-fill--green" style="width: 78%"></div>
                </div>
                <small>평균 1,780 kcal / 목표 1,800 kcal</small>
              </div>
              <div class="stat-card__bar">
                <span>운동량</span>
                <div class="stat-card__bar-track">
                  <div class="stat-card__bar-fill stat-card__bar-fill--blue" style="width: 62%"></div>
                </div>
                <small>주 3회 / 목표 주 4회</small>
              </div>
            </div>
          </div>
        </NnCard>

        <NnCard
          title="이번 주 포인트"
          subtitle="가벼운 리포트 형태로 보여줄 예정"
          class="stat-card"
        >
          <ul class="stat-card__list">
            <li>
              <span>야식 섭취 횟수</span>
              <strong>1회</strong>
            </li>
            <li>
              <span>탄수 비중</span>
              <strong>조금 높은 편</strong>
            </li>
            <li>
              <span>물 섭취량</span>
              <strong>하루 평균 1.6L</strong>
            </li>
          </ul>
        </NnCard>
      </div>
    </section>

    <!-- 아래: 한 달 식단 요약 카드 -->
    <NnCard class="month-summary-card">
      <h2 class="month-summary-card__title">이번 달 식단 개요</h2>
      <p class="month-summary-card__subtitle">
        더블클릭하면 상세 식단(아침/점심/저녁)을 볼 수 있는 형태로 나중에 확장할 예정입니다.
      </p>

      <div class="month-summary-card__grid">
        <div
          v-for="day in exampleDays"
          :key="day.date"
          class="day-card"
        >
          <div class="day-card__date">{{ day.date }}</div>
          <div class="day-card__kcal">{{ day.kcal }} kcal</div>
          <div class="day-card__tag">{{ day.tag }}</div>
        </div>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import NnButton from '../components/common/NnButton.vue';
import NnCard from '../components/common/NnCard.vue';

// TODO: 나중에 실제 /api/meal-plans 응답으로 교체할 더미 데이터
const exampleDays = [
  { date: '12/01(월)', kcal: 1800, tag: '아·점·저 균형식' },
  { date: '12/02(화)', kcal: 1750, tag: '탄수 조금 낮춤' },
  { date: '12/03(수)', kcal: 1900, tag: '운동 후 고단백' },
  { date: '12/04(목)', kcal: 1700, tag: '저녁 야식 컷' },
];
</script>

<style scoped>
.page {
  max-width: 1020px;
  margin: 0 auto;
  padding: 18px 8px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 상단 헤더 */
.page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page__header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.page__header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

/* 가운데 그리드 */
.page__top-grid {
  display: grid;
  grid-template-columns: 2.1fr 1.3fr;
  gap: 16px;
}

/* 히어로 카드 */
.hero-card {
  padding: 0;
}

.hero-card__inner {
  display: flex;
  gap: 16px;
  padding: 18px 20px;
  background: radial-gradient(circle at 20% 0%, #bbf7d0 0, #ecfeff 45%, #ffffff 100%);
  border-radius: 12px;
}

.hero-card__character {
  flex: 0 0 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.hero-card__circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: linear-gradient(145deg, #22c55e, #16a34a);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 68px;
  box-shadow:
    0 12px 30px rgba(22, 163, 74, 0.5),
    inset 0 -6px 12px rgba(15, 118, 110, 0.4);
}

.hero-card__speech {
  font-size: 14px;
  color: #047857;
}

.hero-card__speech strong {
  font-weight: 800;
}

.hero-card__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.hero-card__row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
}

.hero-card__row--highlight {
  border: 1px solid #bbf7d0;
  background: #ecfdf5;
}

.hero-card__label {
  color: #6b7280;
}

.hero-card__value {
  font-weight: 600;
  color: #111827;
}

.hero-card__value--good {
  color: #16a34a;
}

/* 오른쪽 통계 카드 */
.stat-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  height: 100%;
}

.stat-card__main {
  display: flex;
  gap: 12px;
}

.stat-card__percent {
  min-width: 90px;
  text-align: center;
}

.stat-card__percent-value {
  display: block;
  font-size: 26px;
  font-weight: 800;
  color: #4f46e5;
}

.stat-card__percent-label {
  font-size: 12px;
  color: #6b7280;
}

.stat-card__bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card__bar > span {
  font-size: 12px;
  color: #4b5563;
}

.stat-card__bar-track {
  margin-top: 3px;
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.stat-card__bar-fill {
  height: 100%;
  border-radius: 999px;
}

.stat-card__bar-fill--green {
  background: #22c55e;
}

.stat-card__bar-fill--blue {
  background: #3b82f6;
}

.stat-card__bar small {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
}

/* 리스트 카드 */
.stat-card__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.stat-card__list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card__list span {
  color: #4b5563;
}

.stat-card__list strong {
  color: #111827;
}

/* 아래 한 달 요약 카드 */
.month-summary-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.month-summary-card__subtitle {
  margin: 4px 0 12px;
  font-size: 13px;
  color: #6b7280;
}

.month-summary-card__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.day-card {
  min-width: 150px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  font-size: 12px;
}

.day-card__date {
  font-weight: 600;
  color: #111827;
}

.day-card__kcal {
  margin-top: 2px;
  font-weight: 600;
  color: #4b5563;
}

.day-card__tag {
  margin-top: 3px;
  color: #6b7280;
}
</style>
