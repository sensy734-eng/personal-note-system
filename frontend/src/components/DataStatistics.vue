<template>
  <div class="statistics-container">
    <el-row :gutter="20">
      <el-col :span="8"><el-card shadow="hover" class="stat-card"><div class="stat-title">总笔记数</div><div class="stat-value">{{ stats.totalNotes || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover" class="stat-card"><div class="stat-title">星标收藏</div><div class="stat-value" style="color: #e6a23c;">{{ stats.totalStarred || 0 }}</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover" class="stat-card"><div class="stat-title">分类数量</div><div class="stat-value" style="color: #67c23a;">{{ stats.totalCategories || 0 }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12"><el-card shadow="hover"><div ref="pieChartRef" style="height: 300px;"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="hover"><div ref="lineChartRef" style="height: 300px;"></div></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="chart-header">🔥 高频搜索词</div></template>
          <div ref="barChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="chart-header">🕒 创作时段分布 (0-23时)</div></template>
          <div ref="hourChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import request from '../utils/request';
import { ElMessage } from 'element-plus';

const stats = ref({});
const pieChartRef = ref(null);
const lineChartRef = ref(null);
const barChartRef = ref(null);
const hourChartRef = ref(null);
let charts = [];

const initCharts = () => {
  if (pieChartRef.value) {
    const pie = echarts.init(pieChartRef.value);
    pie.setOption({ title: { text: '分类内容占比', left: 'center' }, tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: '50%', data: stats.value.pieData }] });
    charts.push(pie);
  }
  if (lineChartRef.value) {
    const line = echarts.init(lineChartRef.value);
    line.setOption({ title: { text: '创作趋势', left: 'center' }, xAxis: { type: 'category', data: stats.value.trendDates }, yAxis: { type: 'value' }, series: [{ data: stats.value.trendCounts, type: 'line', smooth: true }] });
    charts.push(line);
  }
  // 🚀 高频词柱状图
  if (barChartRef.value) {
    const bar = echarts.init(barChartRef.value);
    bar.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: (stats.value.hotKeywords || []).map(k => k.name).reverse() },
      series: [{ data: (stats.value.hotKeywords || []).map(k => k.value).reverse(), type: 'bar', itemStyle: { color: '#f56c6c' } }]
    });
    charts.push(bar);
  }
  // 🚀 时段分布折线/面积图
  if (hourChartRef.value) {
    const hour = echarts.init(hourChartRef.value);
    hour.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: Array.from({length: 24}, (_, i) => i + '点') },
      yAxis: { type: 'value' },
      series: [{ data: stats.value.hourlyStats, type: 'line', smooth: true, areaStyle: {} }]
    });
    charts.push(hour);
  }
};

const fetchStatistics = async () => {
  try {
    stats.value = await request.get('/statistics');
    await nextTick();
    initCharts();
  } catch (error) { ElMessage.error('加载失败'); }
};

onMounted(fetchStatistics);
onBeforeUnmount(() => charts.forEach(c => c.dispose()));
</script>

<style scoped>
.stat-card { text-align: center; padding: 10px 0; }
.stat-value { font-size: 26px; font-weight: bold; }
.chart-header { font-weight: bold; color: #606266; }
</style>