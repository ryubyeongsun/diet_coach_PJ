<template>
  <div class="card">
    <h2 class="section-title">오늘의 식단 미리보기</h2>
    <div class="placeholder">
      <div class="placeholder-icon">🍱</div>
      <p>오늘의 식단 요약 기능이 여기에 표시될 예정입니다.</p>
      <span>(Coming Soon)</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  summary: Object, // Prop for future data
});
</script>

<style scoped>
.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
}
.card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 20px;
  border-radius: 10px;
  background-color: #f9fafb;
  color: #9ca3af;
}
.placeholder-icon {
  font-size: 40px;
  margin-bottom: 16px;
}
.placeholder p {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
}
.placeholder span {
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
}
</style>
