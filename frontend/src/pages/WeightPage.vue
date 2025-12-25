<template>
  <div class="weight-record-page">
    <div class="page-header">
      <h1 class="page-title">체중 기록</h1>
      <p class="page-description">매일 체중을 기록하고 목표를 향해 나아가세요.</p>
    </div>
    
    <div class="content-wrapper">
      <!-- 좌측: 입력 + 통계 -->
      <div class="left-panel">
        <WeightInputCard @weight-saved="handleDataChange" />
        <WeeklyStats 
          :weekly-data="weeklyData" 
          :goal-weight="currentUser?.targetWeight || 70"
        />
      </div>
      
      <!-- 우측: 달력 -->
      <div class="right-panel">
        <WeightCalendar 
          :records="monthlyRecords"
          :year="currentYear"
          :month="currentMonth"
          @day-selected="handleDaySelected"
          @prev-month="prevMonth"
          @next-month="nextMonth"
        />
      </div>
    </div>
    
    <!-- 수정/삭제 모달 -->
    <WeightEditModal
      :is-open="isModalOpen"
      :selected-date="selectedDate"
      :current-weight="selectedWeight"
      @close="closeModal" 
      @updated="handleDataChange" 
      @deleted="handleDataChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { getWeights } from '../api/weightApi';
import { getCurrentUser } from '../utils/auth';
import { format, startOfMonth, endOfMonth, subDays, addMonths, subMonths } from 'date-fns';

import WeightInputCard from '../components/weight/WeightInputCard.vue';
import WeeklyStats from '../components/weight/WeeklyStats.vue';
import WeightCalendar from '../components/weight/WeightCalendar.vue';
import WeightEditModal from '../components/weight/WeightEditModal.vue';

const currentUser = ref(getCurrentUser());
const monthlyRecords = ref([]);
const weeklyData = ref([]);

// Calendar State
const currentDate = ref(new Date());
const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonth = computed(() => currentDate.value.getMonth() + 1);

// Modal State
const isModalOpen = ref(false);
const selectedDate = ref('');
const selectedWeight = ref(0);

async function loadData() {
  if (!currentUser.value) return;

  // 1. Fetch Monthly Data (for Calendar)
  const monthStart = startOfMonth(currentDate.value);
  const monthEnd = endOfMonth(currentDate.value);
  
  try {
    const records = await getWeights(currentUser.value.id, {
      from: format(monthStart, 'yyyy-MM-dd'),
      to: format(monthEnd, 'yyyy-MM-dd')
    });
    monthlyRecords.value = records;
  } catch (error) {
    console.error("Failed to load monthly records", error);
  }

  // 2. Fetch Weekly Data (for Stats - Last 7 days)
  const weekEnd = new Date();
  const weekStart = subDays(weekEnd, 6);
  
  try {
    const records = await getWeights(currentUser.value.id, {
      from: format(weekStart, 'yyyy-MM-dd'),
      to: format(weekEnd, 'yyyy-MM-dd')
    });
    // Add display labels and reverse to have chronological order (Oldest -> Newest)
    weeklyData.value = records.map(r => ({
      ...r,
      dateLabel: format(new Date(r.recordDate), 'MM/dd')
    })).reverse();
  } catch (error) {
    console.error("Failed to load weekly records", error);
  }
}

function handleDataChange() {
  loadData();
}

function handleDaySelected(day) {
  selectedDate.value = day.dateStr;
  selectedWeight.value = day.weight || 0; // 0 if new entry
  isModalOpen.value = true;
}

function closeModal() {
  isModalOpen.value = false;
  selectedDate.value = '';
  selectedWeight.value = 0;
}

function prevMonth() {
  currentDate.value = subMonths(currentDate.value, 1);
}

function nextMonth() {
  currentDate.value = addMonths(currentDate.value, 1);
}

watch(currentDate, loadData);

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.weight-record-page {
  min-height: 100vh;
  /* background: linear-gradient(135deg, #E8F5E9 0%, #F1F8E9 100%); */
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #2E7D32;
  margin-bottom: 8px;
  margin-top: 0;
}

.page-description {
  font-size: 16px;
  color: #558B2F;
  margin: 0;
}

.content-wrapper {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 1024px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .weight-record-page {
    padding: 16px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .page-description {
    font-size: 14px;
  }
}
</style>
