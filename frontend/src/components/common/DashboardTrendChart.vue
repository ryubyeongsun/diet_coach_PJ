<template>
  <div>
    <h3>섭취 칼로리 vs 목표</h3>
    <canvas ref="calChart"></canvas>

    <h3 style="margin-top:24px;">체중 변화</h3>
    <canvas ref="weightChart"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Chart } from "chart.js/auto";

const calChart = ref(null);
const weightChart = ref(null);

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

  const labels = dayTrends.map(d => d.date);
  const totalCalories = dayTrends.map(d => d.totalCalories ?? null);
  const targetCalories = dayTrends.map(d => d.targetCalories ?? null);
  const weights = dayTrends.map(d => d.weight ?? null);

  calInstance = new Chart(calChart.value, {
    type: "line",
    data: {
      labels,
      datasets: [
        { label: "섭취 칼로리", data: totalCalories },
        { label: "목표 칼로리", data: targetCalories }
      ]
    },
    options: {
      responsive: true,
      spanGaps: true
    }
  });

  weightInstance = new Chart(weightChart.value, {
    type: "line",
    data: {
      labels,
      datasets: [
        { label: "체중(kg)", data: weights }
      ]
    },
    options: {
      responsive: true,
      spanGaps: true
    }
  });
});

onBeforeUnmount(() => {
  if (calInstance) calInstance.destroy();
  if (weightInstance) weightInstance.destroy();
});
</script>
