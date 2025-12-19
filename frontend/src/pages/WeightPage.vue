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
      <div v-else class="prompt-card">
        ✍️ 오늘 체중을 기록하고 변화를 지켜보세요!
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
      <div v-else>
        <ul class="record-list">
          <li v-for="r in displayedRecords" :key="r.id" class="record-item">
            <span class="record-item__date">{{ r.recordDate }}</span>
            <strong class="record-item__weight">{{ r.weight }} kg</strong>
            <span v-if="r.memo" class="record-item__memo">({{ r.memo }})</span>
            <button @click="handleDelete(r.id)" class="delete-btn">삭제</button>
          </li>
        </ul>
        <div class="summary-footer" v-if="records.length > 3">
          <small v-if="!showAllRecords">최근 30일 기록 중 3개만 표시됩니다.</small>
          <button @click="showAllRecords = !showAllRecords" class="view-all-btn">
            {{ showAllRecords ? '간략히 보기' : '전체 기록 보기 &rarr;' }}
          </button>
        </div>
      </div>
    </NnCard>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import NnCard from '../components/common/NnCard.vue';
import NnButton from '../components/common/NnButton.vue';
import NnInput from '../components/common/NnInput.vue';
import TrendChart from '../components/dashboard/TrendChart.vue';
import { upsertWeight, fetchWeights, deleteWeight } from '../api/weightApi.js';
import { fetchDashboardTrend } from '../api/dashboardApi.js';
import { getCurrentUser } from '@/utils/auth.js';

const currentUser = ref(null);

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
const showAllRecords = ref(false); // 전체/요약 보기 토글 상태

// --- 트렌드 ---
const trend = ref(null);
const isTrendLoading = ref(false);
const trendError = ref('');

const todaysRecord = computed(() => {
  const todayStr = new Date().toISOString().slice(0, 10);
  return records.value.find(r => r.recordDate === todayStr);
});

// "전체/요약 보기" 상태에 따라 보여줄 기록을 결정하는 computed 속성
const displayedRecords = computed(() => {
  if (showAllRecords.value) {
    return records.value;
  }
  return records.value.slice(0, 3);
});


async function loadTrendData() {
  const userId = currentUser.value?.id;
  if (!userId) return;
  isTrendLoading.value = true;
  trendError.value = '';
  try {
    const today = new Date();
    const fromDate = new Date();
    fromDate.setDate(today.getDate() - 29);
    
    const to = today.toISOString().slice(0, 10);
    const from = fromDate.toISOString().slice(0, 10);

    trend.value = await fetchDashboardTrend(userId, from, to);
    console.log('Fetched trend data:', trend.value);
  } catch (err) {
    console.warn('Trend API error on WeightPage:', err);
    trendError.value = '트렌드 데이터를 불러오는 중 오류가 발생했습니다.';
  } finally {
    isTrendLoading.value = false;
  }
}

async function loadRecords() {
  const userId = currentUser.value?.id;
  if (!userId) return;
  isLoadingList.value = true;
  listError.value = '';
  try {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 29);
    
    const params = {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10),
    };

    // API로부터 받은 데이터를 날짜 내림차순으로 정렬
        const fetchedRecords = await fetchWeights(userId, params);
        records.value = fetchedRecords.sort((a, b) => new Date(b.recordDate) - new Date(a.recordDate));
        console.log('Fetched weight records:', records.value);
      } catch (err) {
    console.error('fetchWeights error:', err);
    listError.value = '체중 기록을 불러오는 중 오류가 발생했습니다.';
  } finally {
    isLoadingList.value = false;
  }
}

async function onClickSave() {
  const userId = currentUser.value?.id;
  if (!userId) {
    saveError.value = '로그인이 필요합니다.';
    return;
  }
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
    await upsertWeight(userId, payload);
    alert('체중이 기록되었습니다.');
    
    form.value.weight = '';
    form.value.memo = '';
    await Promise.all([loadRecords(), loadTrendData()]);

  } catch (err) {
    console.error('upsertWeight error:', err);
    saveError.value = '체중 기록 중 오류가 발생했습니다.';
  } finally {
    isSaving.value = false;
  }
}

async function handleDelete(recordId) {
  if (!window.confirm('이 기록을 정말로 삭제하시겠습니까?')) {
    return;
  }
  
  const userId = currentUser.value?.id;
  if (!userId) {
    alert('로그인이 필요합니다.');
    return;
  }

  try {
    await deleteWeight(userId, recordId);
    alert('기록이 삭제되었습니다.');
    await Promise.all([loadRecords(), loadTrendData()]);
  } catch (err) {
    console.error('deleteWeight error:', err);
    alert('기록 삭제 중 오류가 발생했습니다.');
  }
}

onMounted(async () => {
  currentUser.value = getCurrentUser();
  if (currentUser.value) {
    await Promise.all([loadRecords(), loadTrendData()]);
  }
});
</script>

<style scoped>
/* ... (기존 스타일과 동일) ... */
.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
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
.record-item:not(:last-child) {
  margin-bottom: 8px;
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
  flex: 1;
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.delete-btn {
  margin-left: auto;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #ffffff;
  color: #dc2626;
  cursor: pointer;
  font-size: 12px;
}
.delete-btn:hover {
  background-color: #fef2f2;
}
.prompt-card {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  font-size: 13px;
  color: #0369a1;
  text-align: center;
}
.summary-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}
.view-all-btn {
  font-size: 13px;
  font-weight: 500;
  color: #2563eb;
  text-decoration: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.view-all-btn:hover {
  text-decoration: underline;
}
/* ... (나머지 스타일) ... */
</style>
