<template>
  <div ref="el" class="chart-box" :style="{ height }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(defineProps<{ option: Record<string, any>; height?: string }>(), {
  height: '300px'
})

const el = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option, true)
}

function resize() {
  chart?.resize()
}

watch(() => props.option, render, { deep: true })

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart-box { width: 100%; }
</style>
