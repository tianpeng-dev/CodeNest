<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import { getSensitiveWords } from '@/services/admin.service';
import type { SensitiveWord } from '@/types/admin';

const rows = ref<SensitiveWord[]>([]);
const loading = ref(false);

const levelLabel = {
  low: '低',
  medium: '中',
  high: '高',
};

const formatDate = (value: string) => value.slice(0, 10);

const columns: AdminTableColumn<SensitiveWord>[] = [
  { key: 'word', label: '敏感词', minWidth: 160 },
  {
    key: 'level',
    label: '等级',
    width: 100,
    tag: (row) => ({
      label: levelLabel[row.level],
      type: row.level === 'high' ? 'danger' : row.level === 'medium' ? 'warning' : 'info',
    }),
  },
  { key: 'hitCount', label: '命中', width: 100, align: 'right' },
  { key: 'createdAt', label: '创建时间', minWidth: 140, formatter: (row) => formatDate(row.createdAt) },
];

const actions: AdminRowAction<SensitiveWord>[] = [
  {
    label: '降低等级',
    type: 'warning',
    confirmText: (row) => `确认降低“${row.word}”的敏感等级？`,
    successText: '敏感词等级已更新',
    disabled: (row) => row.level === 'low',
    onConfirm: (row) => {
      row.level = row.level === 'high' ? 'medium' : 'low';
    },
  },
  {
    label: '移除',
    type: 'danger',
    confirmText: (row) => `确认从本地列表移除“${row.word}”？`,
    successText: '敏感词已移除',
    onConfirm: (row) => {
      rows.value = rows.value.filter((item) => item.id !== row.id);
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = await getSensitiveWords();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <AdminTablePage
    title="敏感词"
    description="查看敏感词等级、命中次数和创建时间，操作仅影响当前页面。"
    :rows="rows"
    :columns="columns"
    :actions="actions"
    :loading="loading"
  />
</template>
