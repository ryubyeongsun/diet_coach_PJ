<template>
  <div class="weekly-stats">
    <h3 class="stats-title">이번 주 요약</h3>
    
    <div class="stat-item">
      <span class="stat-label">평균 체중</span>
      <span class="stat-value">{{ weeklyAverage }} kg</span>
    </div>
    
    <div class="stat-item">
      <span class="stat-label">이번 주 변화</span>
      <span class="stat-value" :class="changeClass">
        {{ weeklyChange >= 0 ? '+' : '' }}{{ weeklyChange }} kg
        {{ weeklyChange < 0 ? '↓' : weeklyChange > 0 ? '↑' : '→' }}
      </span>
    </div>
    
    <div class="stat-item">
      <span class="stat-label">목표까지</span>
      <span class="stat-value">{{ goalRemaining }} kg</span>
    </div>
    
    <div class="mini-chart">
      <canvas ref="chartCanvas"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import Chart from 'chart.js/auto';

const props = defineProps({
  weeklyData: {
    type: Array,
    required: true
  },
  goalWeight: {
    type: Number,
    default: 70
  }
});

const chartCanvas = ref(null);
let chart = null;

const weeklyAverage = computed(() => {
  if (!props.weeklyData.length) return '0.0';
  const sum = props.weeklyData.reduce((acc, item) => acc + item.weight, 0);
  return (sum / props.weeklyData.length).toFixed(1);
});

const weeklyChange = computed(() => {
  if (props.weeklyData.length < 2) return 0;
  const first = props.weeklyData[0].weight;
  const last = props.weeklyData[props.weeklyData.length - 1].weight;
  return (last - first).toFixed(1);
});

const goalRemaining = computed(() => {
  if (!props.weeklyData.length) return '0.0';
  const current = props.weeklyData[props.weeklyData.length - 1].weight;
  // If current > goal, need to lose. If current < goal, need to gain. 
  // We just show absolute difference.
  return Math.abs(current - props.goalWeight).toFixed(1);
});

const changeClass = computed(() => {
  return {
    'decrease': weeklyChange.value < 0,
    'increase': weeklyChange.value > 0
  };
});

function initChart() {
  if (!chartCanvas.value) return;
  const ctx = chartCanvas.value.getContext('2d');
  
  chart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: props.weeklyData.map(d => d.dateLabel), // Expecting dateLabel or format it here
      datasets: [{
        label: '체중',
        data: props.weeklyData.map(d => d.weight),
        borderColor: '#66BB6A',
        backgroundColor: 'rgba(102, 187, 106, 0.1)',
        borderWidth: 2,
        tension: 0.4,
        fill: true,
        pointRadius: 3,
        pointHoverRadius: 5
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          intersect: false,
        }
      },
      scales: {
        y: {
          display: false, // Hide Y axis for cleaner look
          beginAtZero: false
        },
        x: {
          display: false // Hide X axis
        }
      }
    }
  });
}

function updateChart() {
  if (!chart) return;
  
  chart.data.labels = props.weeklyData.map(d => d.dateLabel || d.recordDate);
  chart.data.datasets[0].data = props.weeklyData.map(d => d.weight);
  chart.update();
}

watch(() => props.weeklyData, () => {
  if (chart) {
    updateChart();
  } else {
    initChart();
  }
}, { deep: true });

onMounted(() => {
  initChart();
});

onBeforeUnmount(() => {
  if (chart) {
    chart.destroy();
  }
});
</script>

<style scoped>
.weekly-stats {
  background: linear-gradient(135deg, #E8F5E9 0%, #F1F8E9 100%);
  border-radius: 16px;
  padding: 20px;
  margin-top: 16px;
}

.stats-title {
  font-size: 16px;
  font-weight: 700;
  color: #2E7D32;
  margin-bottom: 16px;
  margin-top: 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(165, 214, 167, 0.3);
}

.stat-item:last-of-type {
  border-bottom: none;
}

.stat-label {
  color: #558B2F;
  font-size: 14px;
  font-weight: 500;
}

.stat-value {
  color: #2E7D32;
  font-size: 18px;
  font-weight: 700;
}

.stat-value.decrease {
  color: #66BB6A;
}

.stat-value.increase {
  color: #FF9800;
}

.mini-chart {
  height: 100px;
  margin-top: 16px;
  width: 100%;
}
</style>
