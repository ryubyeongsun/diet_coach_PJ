<template>
  <div class="detail">
    <h3 class="detail__title">{{ day.dateLabel }} 식단 상세</h3>
    <ul class="detail__list">
      <li v-for="meal in day.meals" :key="meal.time" class="detail__item">
        <div class="detail__time">{{ meal.time }}</div>
        <div class="detail__menu">{{ meal.menu }}</div>
        <div class="detail__calories">{{ meal.calories }} kcal</div>
      </li>
    </ul>
  </div>
</template>

<script setup>
const props = defineProps({
  day: {
    type: Object,
    required: true,
  },
});
</script>

<style scoped>
.detail {
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
}

.detail__title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.detail__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail__item {
  display: grid;
  grid-template-columns: 60px 1fr 80px;
  align-items: center;
  font-size: 13px;
}

.detail__time {
  font-weight: 600;
  color: #4b5563;
}

.detail__menu {
  color: #111827;
}

.detail__calories {
  text-align: right;
  color: #4b5563;
}
</style>
