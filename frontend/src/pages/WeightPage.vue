<template>
  <div class="page">
    <header class="page__header">
      <h1>체중 기록</h1>
      <p>일일 체중 변화를 기록하고 차트로 확인해 보세요.</p>
    </header>

    <div v-if="error" class="error-banner">
      <p>{{ error }}</p>
    </div>

    <div v-if="loading && weights.length === 0" class="loading-spinner">
      <p>데이터를 불러오는 중입니다...</p>
    </div>

    <div v-else class="weight-layout">
      <!-- Left column is now for today's input -->
      <div class="card">
        <h2>오늘의 체중 ({{ todayDate }})</h2>
        <form @submit.prevent="handleSaveWeight" class="input-form">
          <NnInput
            v-model.number="inputWeight"
            type="number"
            step="0.1"
            placeholder="예: 75.2"
          />
          <NnButton type="submit" :disabled="isSaving">
            {{ isSaving ? "저장 중..." : "저장" }}
          </NnButton>
        </form>
        <p v-if="saveError" class="error-text">{{ saveError }}</p>
      </div>

      <!-- Right column is now for the calendar -->
      <div class="card calendar-area">
        <div class="calendar-header">
          <NnButton @click="goToPreviousMonth" variant="secondary">&lt;</NnButton>
          <h2>{{ year }}년 {{ month }}월</h2>
          <NnButton @click="goToNextMonth" variant="secondary">&gt;</NnButton>
          <NnButton @click="goToToday" class="today-btn">오늘</NnButton>
        </div>
        <WeightCalendar 
          :year="year"
          :month="month"
          :records="weights"
          @click-day="handleDayClick"
        />
      </div>
    </div>
    
    <WeightEditModal 
      :is-open="isEditModalOpen"
      :record="selectedDay?.record"
      :date="selectedDay?.date"
      @close="handleModalClose"
      @updated="handleRecordUpdated"
      @deleted="handleRecordDeleted"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { getWeights, upsertWeight, deleteWeight } from "../api/weightApi.js";
import { getCurrentUser } from "../utils/auth.js";
import {
  format,
  subMonths,
  addMonths,
  startOfMonth,
  endOfMonth,
  getYear,
  getMonth,
} from "date-fns";
import NnInput from "../components/common/NnInput.vue";
import NnButton from "../components/common/NnButton.vue";
import WeightCalendar from "../components/weight/WeightCalendar.vue";
import WeightEditModal from "../components/weight/WeightEditModal.vue";

// --- STATE ---
const currentUser = ref(getCurrentUser());
const weights = ref([]);
const loading = ref(false);
const error = ref("");

// Month navigation state
const currentDate = ref(new Date());

// Today's weight input state
const inputWeight = ref(null);
const isSaving = ref(false);
const saveError = ref("");

// Edit Modal state
const isEditModalOpen = ref(false);
const selectedDay = ref(null); // Will hold { date, record }

// --- COMPUTED ---
const year = computed(() => getYear(currentDate.value));
const month = computed(() => getMonth(currentDate.value) + 1);

const todayDate = computed(() => format(new Date(), "yyyy-MM-dd"));

// --- METHODS ---
async function fetchWeights() {
  if (!currentUser.value?.id) {
    error.value = "사용자 정보를 찾을 수 없습니다.";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const range = {
      from: format(startOfMonth(currentDate.value), "yyyy-MM-dd"),
      to: format(endOfMonth(currentDate.value), "yyyy-MM-dd"),
    };
    weights.value = await getWeights(currentUser.value.id, range);
  } catch (err) {
    console.error("Error fetching weight data:", err);
    error.value = "체중 기록을 불러오는 데 실패했습니다.";
  } finally {
    loading.value = false;
  }
}

async function handleSaveWeight() {
  if (!inputWeight.value || inputWeight.value <= 0) {
    saveError.value = "올바른 체중을 입력해주세요.";
    return;
  }
  if (!currentUser.value?.id) {
    saveError.value = "사용자 정보가 없습니다.";
    return;
  }
  isSaving.value = true;
  saveError.value = "";
  try {
    await upsertWeight(currentUser.value.id, {
      recordDate: todayDate.value,
      weight: inputWeight.value,
    });
    await fetchWeights();
    alert("체중이 저장되었습니다!");
    inputWeight.value = null; // Reset input
  } catch (err) {
    console.error("Error saving weight:", err);
    saveError.value = "체중 저장에 실패했습니다. 다시 시도해주세요.";
  } finally {
    isSaving.value = false;
  }
}

function goToPreviousMonth() {
  currentDate.value = subMonths(currentDate.value, 1);
}

function goToNextMonth() {
  currentDate.value = addMonths(currentDate.value, 1);
}

function goToToday() {
  currentDate.value = new Date();
}

function handleDayClick(day) {
  selectedDay.value = day;
  isEditModalOpen.value = true;
}

function handleModalClose() {
  isEditModalOpen.value = false;
  selectedDay.value = null;
}

async function handleRecordUpdated() {
  handleModalClose();
  await fetchWeights();
  alert("체중이 저장/수정되었습니다.");
}

async function handleRecordDeleted() {
  handleModalClose();
  await fetchWeights();
  alert("기록이 삭제되었습니다.");
}


// --- WATCHERS ---
watch(currentDate, fetchWeights);

// --- LIFECYCLE ---
onMounted(() => {
  fetchWeights();
});
</script>




<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__header {
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.page__header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.page__header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.error-banner {
  background-color: #fef2f2;
  color: #dc2626;
  padding: 1rem;
  border-radius: 8px;
}

.error-text {
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
}

.input-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.loading-spinner {
  text-align: center;
  padding: 40px;
  font-size: 14px;
  color: #6b7280;
}

.weight-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
}

.card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.card h2 {
  margin-top: 0;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
}

.calendar-area {
  grid-column: 2 / 3;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.calendar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.today-btn {
  margin-left: 16px;
}
</style>
