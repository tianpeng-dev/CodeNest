<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  label: string;
  value: string | number;
  delta: string | number;
}>();

const deltaText = computed(() => {
  if (typeof props.delta === 'number') {
    return `${props.delta > 0 ? '+' : ''}${props.delta}%`;
  }

  return props.delta;
});

const deltaTone = computed(() => {
  if (typeof props.delta === 'number') {
    return props.delta >= 0 ? 'positive' : 'negative';
  }

  return String(props.delta).trim().startsWith('-') ? 'negative' : 'positive';
});
</script>

<template>
  <section class="stat-card">
    <span class="stat-card__label">{{ label }}</span>
    <strong>{{ value }}</strong>
    <span class="stat-card__delta" :class="`is-${deltaTone}`">{{ deltaText }}</span>
  </section>
</template>

<style scoped>
.stat-card {
  min-width: 0;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.stat-card__label {
  display: block;
  color: #667085;
  font-size: 12px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 24px;
  line-height: 1.1;
}

.stat-card__delta {
  display: inline-block;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 700;
}

.stat-card__delta.is-positive {
  color: #11845b;
}

.stat-card__delta.is-negative {
  color: #b42318;
}
</style>
