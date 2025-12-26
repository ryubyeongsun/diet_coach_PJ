<template>
  <div class="calendar">
    <div class="calendar-header">
      <button class="nav-button" @click="prevMonth">＜</button>
      <h3 class="current-month">{{ currentMonthText }}</h3>
      <button class="nav-button" @click="nextMonth">＞</button>
    </div>
    
    <div class="calendar-grid">
      <div 
        v-for="day in weekDays" 
        :key="'weekday-' + day"
        class="weekday"
      >
        {{ day }}
      </div>
      
      <div
        v-for="day in calendarDays"
        :key="day.id"
        class="calendar-day"
        :class="{
          'has-data': day.hasData,
          'decreased': day.trend === 'decreased',
          'maintained': day.trend === 'maintained',
          'increased': day.trend === 'increased',
          'selected': isSelected(day),
          'other-month': !day.isCurrentMonth,
          'today': day.isToday
        }"
        @click="selectDay(day)"
      >
        <span class="day-number">{{ day.day }}</span>
        <span v-if="day.weight" class="day-weight">
          {{ day.weight }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { format, subMonths, addMonths } from 'date-fns';

const props = defineProps({
  records: {
    type: Array, // Array of { recordDate: 'YYYY-MM-DD', weight: number }
    default: () => []
  },
  year: {
    type: Number,
    required: true
  },
  month: { // 1-based index
    type: Number,
    required: true
  }
});

const emit = defineEmits(['update:month', 'update:year', 'day-selected', 'prev-month', 'next-month']);

const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
const selectedDate = ref(null);

const currentMonthText = computed(() => {
  return `${props.year}년 ${props.month}월`;
});

// Create a map for fast lookup: "YYYY-MM-DD" -> { weight: ... }
const recordsMap = computed(() => {
  const map = {};
  props.records.forEach(r => {
    map[r.recordDate] = r;
  });
  return map;
});

const calendarDays = computed(() => {
  const year = props.year;
  const monthIndex = props.month - 1; // JS Date is 0-indexed
  
  const firstDay = new Date(year, monthIndex, 1);
  const lastDay = new Date(year, monthIndex + 1, 0);
  const prevLastDay = new Date(year, monthIndex, 0);
  
  const firstDayOfWeek = firstDay.getDay(); // 0 (Sun) - 6 (Sat)
  const lastDate = lastDay.getDate();
  const prevLastDate = prevLastDay.getDate();
  
  const days = [];
  let dayId = 0;
  
  // Previous month filling
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    days.push({
      id: `prev-${dayId++}`,
      day: prevLastDate - i,
      isCurrentMonth: false,
      hasData: false
    });
  }
  
  // Current month
  const today = new Date();
  const todayStr = format(today, "yyyy-MM-dd");

  for (let date = 1; date <= lastDate; date++) {
    const dateStr = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
    const record = recordsMap.value[dateStr];
    
    // Calculate trend
    let trend = null;
    if (record) {
      // Find previous day record for trend
      const prevDate = new Date(dateStr);
      prevDate.setDate(prevDate.getDate() - 1);
      const prevDateStr = format(prevDate, "yyyy-MM-dd");
      const prevRecord = recordsMap.value[prevDateStr];
      
      if (prevRecord) {
        const diff = record.weight - prevRecord.weight;
        if (diff < -0.1) trend = 'decreased';
        else if (diff > 0.1) trend = 'increased';
        else trend = 'maintained';
      }
    }
    
    days.push({
      id: `curr-${dayId++}`,
      day: date,
      isCurrentMonth: true,
      dateStr: dateStr,
      hasData: !!record,
      weight: record?.weight,
      record: record, // Pass full record
      trend: trend,
      isToday: dateStr === todayStr
    });
  }
  
  // Next month filling
  const remainingDays = 42 - days.length; // 6 rows * 7 days = 42
  for (let date = 1; date <= remainingDays; date++) {
    days.push({
      id: `next-${dayId++}`,
      day: date,
      isCurrentMonth: false,
      hasData: false
    });
  }
  
  return days;
});

function prevMonth() {
  emit('prev-month');
}

function nextMonth() {
  emit('next-month');
}

function selectDay(day) {
  if (!day.isCurrentMonth) return;
  // allow selecting empty days too, to add record
  selectedDate.value = day.dateStr;
  emit('day-selected', day);
}

function isSelected(day) {
  return day.dateStr === selectedDate.value;
}
</script>

<style scoped>
.calendar {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.current-month {
  font-size: 20px;
  font-weight: 700;
  color: #2E7D32;
  margin: 0;
}

.nav-button {
  background: #F1F8E9;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 16px;
  color: #2E7D32;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-button:hover {
  background: #E8F5E9;
  transform: scale(1.05);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #7CB342;
  padding: 8px 0;
}

.calendar-day {
  aspect-ratio: 1;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 8px;
  background: #FAFAFA;
  position: relative;
  border: 2px solid transparent;
}

.calendar-day.other-month {
  opacity: 0.3;
  cursor: default;
}

.calendar-day.today {
  border-color: #FF9800;
}

.calendar-day.has-data:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.calendar-day.decreased {
  background: linear-gradient(135deg, #C5E1A5 0%, #AED581 100%);
}

.calendar-day.maintained {
  background: linear-gradient(135deg, #FFF59D 0%, #FFF176 100%);
}

.calendar-day.increased {
  background: linear-gradient(135deg, #FFE082 0%, #FFD54F 100%);
}

.calendar-day.selected {
  border-color: #66BB6A;
  box-shadow: 0 0 0 3px rgba(102, 187, 106, 0.2);
}

.day-number {
  font-size: 14px;
  font-weight: 600;
  color: #37474F;
}

.day-weight {
  font-size: 11px;
  font-weight: 700;
  color: #2E7D32;
  margin-top: 4px;
}
</style>