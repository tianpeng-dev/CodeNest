<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import { getAdminModerators } from '@/services/admin.service';
import type { AdminModeratorSection } from '@/types/admin';

const rows = ref<AdminModeratorSection[]>([]);
const loading = ref(false);

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));

const columns: AdminTableColumn<AdminModeratorSection>[] = [
  { key: 'sectionName', label: '版块', minWidth: 130 },
  { key: 'description', label: '职责说明', minWidth: 280 },
  { key: 'moderatorCount', label: '人数', width: 90, align: 'right' },
  {
    key: 'moderators',
    label: '版主列表',
    minWidth: 220,
    formatter: (row) => row.moderators.map((moderator) => moderator.displayName).join('、'),
  },
  { key: 'updatedAt', label: '更新于', minWidth: 140, formatter: (row) => formatDate(row.updatedAt) },
];

const actions: AdminRowAction<AdminModeratorSection>[] = [
  {
    label: '刷新名单',
    type: 'primary',
    confirmText: (row) => `确认刷新“${row.sectionName}”版主名单状态？`,
    successText: '版主名单已刷新',
    onConfirm: (row) => {
      row.updatedAt = new Date().toISOString();
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = await getAdminModerators();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <AdminTablePage
    title="版主管理"
    description="按版块展示版主名单、职责说明和最近维护时间。"
    :rows="rows"
    :columns="columns"
    :actions="actions"
    :loading="loading"
  />
</template>
