<script setup lang="ts">
import { computed } from 'vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, PieChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components';
import type { EChartsOption } from 'echarts';
import type { PiePoint, TrendPoint } from '@/types/analytics';

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
]);

const props = withDefaults(
  defineProps<{
    type: 'line' | 'pie';
    data: TrendPoint[] | PiePoint[];
    title?: string;
    height?: number;
  }>(),
  {
    title: '',
    height: 300,
  },
);

const option = computed<EChartsOption>(() => {
  if (props.type === 'pie') {
    const data = props.data as PiePoint[];

    return {
      color: ['#3d6f8f', '#d97706', '#16a34a', '#7c3aed', '#dc2626'],
      tooltip: { trigger: 'item' },
      legend: {
        bottom: 0,
        left: 'center',
      },
      series: [
        {
          name: props.title || '占比',
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: true,
          label: {
            formatter: '{b}',
          },
          data,
        },
      ],
    };
  }

  const data = props.data as TrendPoint[];

  return {
    color: ['#3d6f8f'],
    tooltip: { trigger: 'axis' },
    grid: {
      top: 24,
      right: 18,
      bottom: 32,
      left: 42,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#d0d5dd' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf1f7' } },
    },
    series: [
      {
        name: props.title || '趋势',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        areaStyle: {
          color: 'rgba(61, 111, 143, 0.12)',
        },
        lineStyle: {
          width: 3,
        },
        data: data.map((item) => item.value),
      },
    ],
  };
});
</script>

<template>
  <VChart class="stats-chart" :option="option" autoresize :style="{ height: `${height}px` }" />
</template>

<style scoped>
.stats-chart {
  width: 100%;
  min-width: 0;
}
</style>
