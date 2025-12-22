<template>
  <div class="card summary-card">
    <h3>주간 체중 변화</h3>
    <div v-if="trend?.days">
      <p :class="weightChangeClass">
        {{ weightChangeText }}
        <span class="unit" v-if="weightChange !== 0">kg</span>
      </p>
      <div class="mini-chart-container">
        <Line v-if="hasEnoughData" :data="chartData" :options="chartOptions" />
      </div>
    </div>
     <div v-else class="placeholder">
      <p>트렌드 정보 없음</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Line } from 'vue-chartjs';
import { Chart as ChartJS, LineElement, CategoryScale, LinearScale, PointElement } from 'chart.js';

ChartJS.register(LineElement, CategoryScale, LinearScale, PointElement);

const props = defineProps({
  trend: Object,
});

const weightDataPoints = computed(() => {
  if (!props.trend?.days) return [];
  return props.trend.days
    .map(d => ({ date: d.date, weight: d.weight }))
    .filter(d => d.weight !== null)
    .sort((a, b) => new Date(a.date) - new Date(b.date));
});

const hasEnoughData = computed(() => weightDataPoints.value.length >= 2);

const weightChange = computed(() => {
  if (!hasEnoughData.value) return 0;
  const first = weightDataPoints.value[0].weight;
  const last = weightDataPoints.value[weightDataPoints.value.length - 1].weight;
  return last - first;
});

const weightChangeText = computed(() => {
  const change = weightChange.value;
  if (change > 0) return `+${change.toFixed(1)}`;
  if (change < 0) return change.toFixed(1);
  return '변화 없음';
});

const weightChangeClass = computed(() => {
  const change = weightChange.value;
  if (change > 0) return 'positive';
  if (change < 0) return 'negative';
  return '';
});

const chartData = computed(() => ({
  labels: weightDataPoints.value.map(d => d.date),
  datasets: [{
    data: weightDataPoints.value.map(d => d.weight),
    borderColor: weightChange.value <= 0 ? '#22c55e' : '#ef4444',
    tension: 0.3,
    borderWidth: 2,
    pointRadius: 0,
  }],
}));

const chartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { display: false },
    y: { display: false },
  },
});
</script>

<style scoped>
.summary-card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
h3 {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
  color: #4b5563;
}
p {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}
.unit {
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
}
.positive { color: #ef4444; }
.negative { color: #22c55e; }
.mini-chart-container {
  height: 50px;
  margin-top: 12px;
}
.placeholder {
  color: #9ca3af;
}
</style>
