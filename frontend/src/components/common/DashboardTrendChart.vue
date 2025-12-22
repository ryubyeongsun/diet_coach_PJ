<template>
  <div v-if="isDataSufficient">
    <h3>섭취 칼로리 vs 목표</h3>
    <canvas ref="calChart"></canvas>

    <h3 style="margin-top: 24px">체중 변화</h3>
    <canvas ref="weightChart"></canvas>
  </div>
  <div v-else class="insufficient-data-notice">
    <p>아직 데이터가 충분하지 않습니다.</p>
    <p>2일 이상 기록하면 그래프를 확인할 수 있어요.</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Chart } from "chart.js/auto";

const calChart = ref(null);
const weightChart = ref(null);
const isDataSufficient = ref(true); // 데이터가 충분한지 여부를 제어하는 상태

let calInstance = null;
let weightInstance = null;

async function fetchTrend() {
  const userId = 1;
  const res = await fetch(`/api/dashboard/trend?userId=${userId}`);
  const json = await res.json();
  return json.data.dayTrends ?? [];
}

onMounted(async () => {
  const dayTrends = await fetchTrend();

  // PRD 요구사항: 데이터 포인트가 2개 미만이면 차트를 표시하지 않음
  if (dayTrends.length < 2) {
    isDataSufficient.value = false;
    return;
  }

  const labels = dayTrends.map((d) => d.date);
  const totalCalories = dayTrends.map((d) => d.totalCalories ?? null);
  const targetCalories = dayTrends.map((d) => d.targetCalories ?? null);
  const weights = dayTrends.map((d) => d.weight ?? null);

  calInstance = new Chart(calChart.value, {
    type: "line",
    data: {
      labels,
      datasets: [
        { label: "섭취 칼로리", data: totalCalories },
        { label: "목표 칼로리", data: targetCalories },
      ],
    },
    options: {
      responsive: true,
      spanGaps: true,
    },
  });

  weightInstance = new Chart(weightChart.value, {
    type: "line",
    data: {
      labels,
      datasets: [{ label: "체중(kg)", data: weights }],
    },
    options: {
      responsive: true,
      spanGaps: true,
    },
  });
});

onBeforeUnmount(() => {
  if (calInstance) calInstance.destroy();
  if (weightInstance) weightInstance.destroy();
});
</script>

<style scoped>
.insufficient-data-notice {
  padding: 40px 20px;
  text-align: center;
  color: #666;
  border: 1px solid #eee;
  border-radius: 8px;
  background-color: #fafafa;
  margin-top: 16px;
}
.insufficient-data-notice p {
  margin: 0;
}
</style>
