<template>
  <div class="page">
    <header class="page__header">
      <h1>체중 기록</h1>
      <p>오늘 체중을 기록하고, 최근 변화를 확인해 보세요.</p>
    </header>
    
    <NnCard title="최근 30일 체중 변화">
      <div v-if="isTrendLoading" class="page__status">
        트렌드 데이터를 불러오는 중입니다...
      </div>
      <div v-else-if="trendError" class="page__error">
        {{ trendError }}
      </div>
      <TrendChart
        v-else-if="trend"
        :day-trends="trend.dayTrends"
        :show-calories="false"
        period-label="최근 30일"
      />
    </NnCard>

    <NnCard title="오늘 체중 기록하기">
      <div v-if="todaysRecord" class="todays-record-info">
        💡 오늘 기록된 체중: <strong>{{ todaysRecord.weight }}kg</strong>. 다시 저장하면 덮어씁니다.
      </div>

      <form @submit.prevent="onClickSave" class="form">
        <div class="form-group">
          <label for="recordDate">날짜</label>
          <input type="date" id="recordDate" v-model="form.recordDate" class="form-input" />
        </div>
        <div class="form-group">
          <label for="weight">체중 (kg)</label>
          <NnInput id="weight" type="number" step="0.1" v-model="form.weight" placeholder="예: 73.2" />
        </div>
        <div class="form-group">
          <label for="memo">메모</label>
          <textarea id="memo" v-model="form.memo" class="form-textarea" rows="3" placeholder="(선택) 오늘 식단이나 운동에 대한 메모를 남겨보세요."></textarea>
        </div>

        <p v-if="saveError" class="page__error">{{ saveError }}</p>

        <div class="form-actions">
          <NnButton type="submit" :disabled="isSaving">
            {{ isSaving ? '저장 중...' : (todaysRecord ? '수정하기' : '기록하기') }}
          </NnButton>
        </div>
      </form>
    </NnCard>

    <NnCard title="최근 30일 기록">
      <div v-if="isLoadingList" class="page__status">기록을 불러오는 중입니다...</div>
      <div v-else-if="listError" class="page__error">{{ listError }}</div>
      <div v-else-if="!records || records.length === 0" class="page__status">아직 체중 기록이 없습니다.</div>
      <ul v-else class="record-list">
        <li v-for="r in records" :key="r.id" class="record-item">
          <span class="record-item__date">{{ r.recordDate }}</span>
          <strong class="record-item__weight">{{ r.weight }} kg</strong>
          <span v-if="r.memo" class="record-item__memo">({{ r.memo }})</span>
        </li>
      </ul>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import NnCard from '../components/common/NnCard.vue';
import NnButton from '../components/common/NnButton.vue';
import NnInput from '../components/common/NnInput.vue';
import TrendChart from '../components/dashboard/TrendChart.vue';
import { createWeight, fetchWeights } from '../api/weightApi.js';
import { fetchDashboardTrend } from '../api/dashboardApi.js';

const userId = Number(localStorage.getItem('userId') || 1);

const form = ref({
  recordDate: new Date().toISOString().slice(0, 10),
  weight: '',
  memo: '',
});

const isSaving = ref(false);
const saveError = ref('');

const isLoadingList = ref(false);
const listError = ref('');
const records = ref([]);

// --- 트렌드 ---
const trend = ref(null);
const isTrendLoading = ref(false);
const trendError = ref('');

const todaysRecord = computed(() => {
  return records.value.find(r => r.recordDate === form.value.recordDate);
});

async function loadTrendData() {
  if (!userId) return;
  isTrendLoading.value = true;
  trendError.value = '';
  try {
    const today = new Date();
    const fromDate = new Date();
    fromDate.setDate(today.getDate() - 29);
    
    const to = today.toISOString().slice(0, 10);
    const from = fromDate.toISOString().slice(0, 10);

    trend.value = await fetchDashboardTrend(userId, { from, to });
  } catch (err) {
    console.warn('Trend API error on WeightPage, using mock data', err);
    trendError.value = '트렌드 데이터를 불러오는 중 오류가 발생했습니다. 임시 데이터로 표시합니다.';
    
    const mockTrends = [];
    const today = new Date();
    for (let i = 29; i >= 0; i--) {
      const date = new Date();
      date.setDate(today.getDate() - i);
      mockTrends.push({
        date: date.toISOString().slice(0, 10),
        totalCalories: 0, // Not shown
        targetCalories: 0, // Not shown
        weight: 72.5 + (Math.random() - 0.5) * 2,
      });
    }
    trend.value = { dayTrends: mockTrends };

  } finally {
    isTrendLoading.value = false;
  }
}

async function loadRecords() {
  isLoadingList.value = true;
  listError.value = '';
  try {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 30);
    
    const params = {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10),
    };

    records.value = await fetchWeights(userId, params);
  } catch (err) {
    console.error('fetchWeights error:', err);
    listError.value = '체중 기록을 불러오는 중 오류가 발생했습니다.';
  } finally {
    isLoadingList.value = false;
  }
}

async function onClickSave() {
  if (!form.value.weight) {
    alert('체중을 입력해 주세요.');
    return;
  }

  isSaving.value = true;
  saveError.value = '';

  try {
    const payload = {
      recordDate: form.value.recordDate,
      weight: Number(form.value.weight),
      memo: form.value.memo || null,
    };
    await createWeight(userId, payload);
    alert('체중이 기록되었습니다.');
    
    // 폼 초기화 및 리스트/차트 새로고침
    form.value.weight = '';
    form.value.memo = '';
    await Promise.all([loadRecords(), loadTrendData()]);

  } catch (err) {
    console.error('createWeight error:', err);
    saveError.value = '체중 기록 중 오류가 발생했습니다.';
  } finally {
    isSaving.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadRecords(), loadTrendData()]);
});
</script>

<style scoped>
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.page__header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.page__error {
  margin-top: 8px;
  font-size: 13px;
  color: #dc2626;
}

.page__status {
  padding: 16px 0;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background-color: #ffffff;
}

.form-input:focus, .form-textarea:focus {
  outline: 2px solid transparent;
  outline-offset: 2px;
  border-color: #4f46e5;
  box-shadow: 0 0 0 2px #c7d2fe;
}

.form-actions {
  margin-top: 8px;
  text-align: right;
}

.record-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background-color: #f9fafb;
  border-radius: 8px;
  font-size: 14px;
}

.record-item__date {
  font-weight: 500;
  color: #4b5563;
}

.record-item__weight {
  font-weight: 700;
  color: #111827;
}

.record-item__memo {
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todays-record-info {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 8px;
  font-size: 13px;
  color: #4338ca;
}
</style>
