<script setup>
import { computed } from 'vue';
import { Line as LineChart } from 'vue-chartjs';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  BarElement,
  CategoryScale,
  LinearScale,
  PointElement,
} from 'chart.js';

ChartJS.register(
  Title,
  Tooltip,
  Legend,
  LineElement,
  BarElement,
  CategoryScale,
  LinearScale,
  PointElement
);

const props = defineProps({
  dayTrends: {
    type: Array,
    default: () => [],
  },
  periodLabel: {
    type: String,
    default: '최근 데이터',
  },
  showCalories: {
    type: Boolean,
    default: true,
  },
  showWeight: {
    type: Boolean,
    default: true,
  },
});

// PRD 요구사항: 데이터 포인트가 2개 이상일 때만 차트 표시
const isDataSufficient = computed(() => props.dayTrends && props.dayTrends.length >= 2);

const chartData = computed(() => {
  if (!isDataSufficient.value) {
    return { labels: [], datasets: [] };
  }

  const labels = props.dayTrends.map(d => {
    const date = new Date(d.date);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  });

  const datasets = [];

  if (props.showCalories) {
    datasets.push({
      label: '섭취 칼로리',
      data: props.dayTrends.map(d => d.totalCalories),
      backgroundColor: 'rgba(54, 162, 235, 0.6)',
      borderColor: 'rgba(54, 162, 235, 1)',
      type: 'bar',
      yAxisID: 'yCalories',
      order: 2,
    });
    datasets.push({
      label: '목표 칼로리',
      data: props.dayTrends.map(d => d.targetCalories),
      borderColor: 'rgba(255, 99, 132, 1)',
      borderDash: [5, 5],
      type: 'line',
      fill: false,
      yAxisID: 'yCalories',
      order: 1,
    });
  }

  if (props.showWeight) {
    datasets.push({
      label: '체중 (kg)',
      data: props.dayTrends.map(d => d.weight),
      borderColor: 'rgba(75, 192, 192, 1)',
      type: 'line',
      fill: false,
      yAxisID: 'yWeight',
      order: 0,
    });
  }

  return { labels, datasets };
});

const chartOptions = computed(() => {
  const scales = {};
  if (props.showCalories) {
    scales.yCalories = {
      type: 'linear',
      position: 'left',
      title: {
        display: true,
        text: '칼로리 (kcal)',
      },
    };
  }
  if (props.showWeight) {
    scales.yWeight = {
      type: 'linear',
      position: 'right',
      title: {
        display: true,
        text: '체중 (kg)',
      },
      grid: {
        drawOnChartArea: false, // only draw grid for first Y axis
      },
    };
  }

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      title: {
        display: true,
        text: `${props.periodLabel} 섭취량 & 체중`,
      },
      legend: {
        position: 'bottom',
      },
    },
    scales,
  };
});
</script>

<template>
  <div class="trend-chart-container">
    <div v-if="isDataSufficient" class="chart-wrapper">
      <LineChart :data="chartData" :options="chartOptions" />
    </div>
    <!-- PRD 요구사항: 데이터 부족 시 안내 메시지 표준화 -->
    <div v-else class="insufficient-data-notice">
      <p>아직 데이터가 충분하지 않습니다.</p>
      <p>2일 이상 기록하면 그래프를 확인할 수 있어요.</p>
    </div>
  </div>
</template>

<style scoped>
.trend-chart-container {
  position: relative;
  height: 350px; /* 높이 확보 */
}
.chart-wrapper {
  position: relative;
  height: 100%;
}
.insufficient-data-notice {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  color: #666;
  border: 1px solid #eee;
  border-radius: 8px;
  background-color: #fafafa;
}
.insufficient-data-notice p {
  margin: 0;
}
</style>
