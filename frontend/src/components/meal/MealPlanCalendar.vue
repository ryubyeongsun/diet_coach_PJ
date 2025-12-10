<template>
  <div class="calendar">
    <!-- 데이터 없을 때 -->
    <div v-if="!days || days.length === 0" class="calendar__empty">
      아직 생성된 식단이 없습니다. 상단에서 식단을 먼저 생성해 주세요.
    </div>

    <!-- 데이터 있을 때 -->
    <div v-else class="calendar__list">
      <button
        v-for="day in days"
        :key="day.dayId"
        type="button"
        class="calendar__item"
      >
        <p class="calendar__date">
          {{ formatDate(day.date) }}
        </p>
        <p class="calendar__kcal">
          {{ day.totalCalories }} kcal
        </p>
        <p class="calendar__memo">
          {{ day.memo || '상세 식단은 추후 추가 예정입니다.' }}
        </p>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  days: {
    type: Array,
    required: true,
  },
});

const weekday = ['일', '월', '화', '수', '목', '금', '토'];

function formatDate(isoString) {
  if (!isoString) return '';
  const d = new Date(isoString);
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const w = weekday[d.getDay()];
  return `${m}/${day}(${w})`;
}
</script>

<style scoped>
.calendar {
  width: 100%;
}

.calendar__empty {
  padding: 20px 16px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.calendar__list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.calendar__item {
  flex: 0 0 calc(25% - 9px); /* 4개씩 */
  min-width: 160px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.03);
  text-align: left;
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}

.calendar__item:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}

.calendar__date {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.calendar__kcal {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.calendar__memo {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
}
</style>
