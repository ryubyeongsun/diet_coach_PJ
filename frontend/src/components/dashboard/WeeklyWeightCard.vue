<template>
  <div class="stat-card">
    <div class="card-header">주간 체중 변화</div>
    
    <div v-if="trend?.dayTrends">
      <div class="weight-change" :class="weightChangeClass">
        {{ weightChangeText }}
        <span class="unit" v-if="weightChange !== 0">kg</span>
      </div>
      
      <div class="mini-chart-container">
        <Line v-if="hasEnoughData" :data="chartData" :options="chartOptions" />
      </div>
    </div>
    
    <div v-else class="placeholder">
      <p class="placeholder-text">데이터가 쌓이면<br/>그래프가 보여요 📈</p>
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
  if (!props.trend?.dayTrends) return [];
  return props.trend.dayTrends
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
  if (change > 0) return 'increase';
  if (change < 0) return 'decrease';
  return 'neutral';
});

const chartData = computed(() => ({
  labels: weightDataPoints.value.map(d => d.date),
  datasets: [{
    data: weightDataPoints.value.map(d => d.weight),
    borderColor: weightChange.value <= 0 ? '#22C55E' : '#EF4444',
    tension: 0.4, // Smoother curve
    borderWidth: 2,
    pointRadius: 0,
    pointHoverRadius: 0,
    fill: false
  }],
}));

const chartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { enabled: false } },
  scales: {
    x: { display: false },
    y: { display: false },
  },
  layout: {
      padding: { top: 5, bottom: 5, left: 5, right: 5 }
  }
});
</script>

<style scoped>
.stat-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
}

.card-header { 
  font-size: 14px; 
  color: #6B7280; 
  margin-bottom: 8px; 
  font-weight: 500;
}

.weight-change {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 16px;
}
.weight-change.increase { color: #EF4444; } /* Red for weight gain */
.weight-change.decrease { color: #22C55E; } /* Green for weight loss */
.weight-change.neutral { color: #6B7280; }

.unit {
  font-size: 16px;
  font-weight: 500;
  color: #6B7280;
}

.mini-chart-container {
  height: 60px;
  width: 100%;
}

.placeholder {
  color: #9ca3af;
  text-align: center;
  padding: 20px 0;
}
.placeholder-text {
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
</style>

