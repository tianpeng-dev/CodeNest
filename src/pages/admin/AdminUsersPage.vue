<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import { getAdminUsers } from '@/services/admin.service';
import type { User } from '@/types/user';

const rows = ref<User[]>([]);
const loading = ref(false);

const formatDate = (value: string | null) => {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const columns: AdminTableColumn<User>[] = [
  { key: 'displayName', label: '用户', minWidth: 120 },
  { key: 'username', label: '账号', minWidth: 120 },
  {
    key: 'role',
    label: '角色',
    width: 100,
    tag: (row) => ({
      label: row.role === 'admin' ? '管理员' : '用户',
      type: row.role === 'admin' ? 'danger' : 'info',
    }),
  },
  {
    key: 'status',
    label: '状态',
    width: 100,
    tag: (row) => ({
      label: row.status === 'active' ? '正常' : '封禁',
      type: row.status === 'active' ? 'success' : 'danger',
    }),
  },
  { key: 'muteUntil', label: '禁言至', minWidth: 140, formatter: (row) => formatDate(row.muteUntil) },
  { key: 'postCount', label: '帖子', width: 90, align: 'right' },
  { key: 'likeCount', label: '获赞', width: 90, align: 'right' },
];

const actions: AdminRowAction<User>[] = [
  {
    label: '切换状态',
    type: 'warning',
    confirmText: (row) => `确认${row.status === 'active' ? '封禁' : '恢复'} ${row.displayName}？`,
    successText: '用户状态已更新',
    onConfirm: (row) => {
      row.status = row.status === 'active' ? 'banned' : 'active';
    },
  },
  {
    label: '禁言7天',
    type: 'primary',
    confirmText: (row) => `确认将 ${row.displayName} 禁言 7 天？`,
    successText: '禁言时间已更新',
    onConfirm: (row) => {
      const next = new Date();
      next.setDate(next.getDate() + 7);
      row.muteUntil = next.toISOString();
    },
  },
  {
    label: '切换角色',
    type: 'info',
    confirmText: (row) => `确认调整 ${row.displayName} 的角色？`,
    successText: '用户角色已更新',
    onConfirm: (row) => {
      row.role = row.role === 'admin' ? 'user' : 'admin';
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = await getAdminUsers();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <AdminTablePage
    title="用户管理"
    description="查看用户角色、账号状态和禁言时间，操作仅更新当前管理台视图。"
    :rows="rows"
    :columns="columns"
    :actions="actions"
    :loading="loading"
  />
</template>
