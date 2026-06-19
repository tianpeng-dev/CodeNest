<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import { getAdminCategories } from '@/services/admin.service';
import type { Category } from '@/types/post';

type CategoryRow = Category & {
  state: 'active' | 'paused';
};

const rows = ref<CategoryRow[]>([]);
const loading = ref(false);

const columns: AdminTableColumn<CategoryRow>[] = [
  { key: 'name', label: '分类', minWidth: 130 },
  { key: 'slug', label: '标识', minWidth: 150 },
  { key: 'description', label: '描述', minWidth: 280 },
  { key: 'postCount', label: '帖子数', width: 100, align: 'right' },
  {
    key: 'state',
    label: '状态',
    width: 100,
    tag: (row) => ({
      label: row.state === 'active' ? '展示中' : '维护中',
      type: row.state === 'active' ? 'success' : 'warning',
    }),
  },
];

const actions: AdminRowAction<CategoryRow>[] = [
  {
    label: '切换状态',
    type: 'warning',
    confirmText: (row) => `确认切换“${row.name}”分类状态？`,
    successText: '分类状态已更新',
    onConfirm: (row) => {
      row.state = row.state === 'active' ? 'paused' : 'active';
    },
  },
  {
    label: '修正计数',
    type: 'primary',
    confirmText: (row) => `确认将“${row.name}”帖子数加 1？`,
    successText: '帖子计数已更新',
    onConfirm: (row) => {
      row.postCount += 1;
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = (await getAdminCategories()).map((category) => ({
      ...category,
      state: 'active',
    }));
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <AdminTablePage
    title="分类管理"
    description="维护分类描述、帖子计数和展示状态，当前为本地确认演示。"
    :rows="rows"
    :columns="columns"
    :actions="actions"
    :loading="loading"
  />
</template>
