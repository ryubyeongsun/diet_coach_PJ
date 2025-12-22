<template>
  <div class="calendar-grid">
    <div v-for="day in weekdays" :key="day" class="weekday-header">{{ day }}</div>
    
    <!-- Empty cells for padding before the 1st day -->
    <div v-for="pad in paddingDays" :key="`pad-${pad}`" class="calendar-day is-padding"></div>
    
    <!-- Actual days of the month -->
    <div 
      v-for="day in daysInMonth" 
      :key="day.date"
      class="calendar-day"
      :class="{ 
        'is-today': day.isToday, 
        'has-record': day.record,
        'is-future': day.isFuture,
      }"
      @click="onDayClick(day)"
    >
      <span class="day-number">{{ day.dayOfMonth }}</span>
      <div v-if="day.record" class="weight-record">
        {{ day.record.weight }} kg
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { 
  startOfMonth, 
  endOfMonth, 
  eachDayOfInterval,
  getDay, 
  getDate,
  isToday as checkIsToday,
  isFuture as checkIsFuture,
} from 'date-fns';

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true }, // 1-12
  records: { type: Array, default: () => [] },
});

const emit = defineEmits(['click-day']);

const weekdays = ['일', '월', '화', '수', '목', '금', '토'];

const onDayClick = (day) => {
  // Do not allow clicking on future dates
  if (day.isFuture) return;
  emit('click-day', day);
};

const daysInMonth = computed(() => {
  const date = new Date(props.year, props.month - 1);
  const interval = { start: startOfMonth(date), end: endOfMonth(date) };
  
  const recordsMap = new Map();
  props.records.forEach(record => {
    recordsMap.set(record.recordDate, record);
  });
  
  return eachDayOfInterval(interval).map(dayDate => {
    const dateString = `${dayDate.getFullYear()}-${String(dayDate.getMonth() + 1).padStart(2, '0')}-${String(dayDate.getDate()).padStart(2, '0')}`;
    return {
      date: dateString,
      dayOfMonth: getDate(dayDate),
      isToday: checkIsToday(dayDate),
      isFuture: checkIsFuture(dayDate) && !checkIsToday(dayDate),
      record: recordsMap.get(dateString) || null,
    };
  });
});

const paddingDays = computed(() => {
  const firstDayOfMonth = startOfMonth(new Date(props.year, props.month - 1));
  return getDay(firstDayOfMonth); // 0 (Sun) to 6 (Sat)
});
</script>

<style scoped>
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px; /* 간격 줄임 */
}
.weekday-header {
  text-align: center;
  font-weight: 600;
  color: #6b7280;
  padding-bottom: 8px;
  font-size: 13px; /* 폰트 크기 조정 */
}
.calendar-day {
  min-height: 80px; /* 높이 줄임 */
  padding: 6px; /* 패딩 줄임 */
  border: 1px solid #f3f4f6;
  border-radius: 6px; /* border-radius 약간 줄임 */
  background-color: #ffffff;
  font-size: 12px; /* 폰트 크기 조정 */
  transition: background-color 0.2s;
}
.calendar-day:not(.is-padding):not(.is-future) {
  cursor: pointer;
}
.calendar-day:not(.is-padding):not(.is-future):hover {
  background-color: #eff6ff;
}
.calendar-day.is-future {
  background-color: #f9fafb;
  color: #d1d5db;
}
.day-number {
  font-weight: 500;
  color: #374151;
}
.calendar-day.is-today .day-number {
  color: #ffffff;
  background-color: #3b82f6;
  border-radius: 50%;
  padding: 2px 5px;
  font-weight: 700;
}
.calendar-day.has-record {
  background-color: #dbeafe;
  border-color: #93c5fd;
}
.weight-record {
  margin-top: 8px;
  font-weight: 700;
  color: #1e40af;
  font-size: 14px;
}
</style>
