<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import { getAdminPosts } from '@/services/admin.service';
import type { Post } from '@/types/post';

const rows = ref<Post[]>([]);
const loading = ref(false);

const statusLabel = {
  draft: '草稿',
  published: '已发布',
  hidden: '已隐藏',
  deleted: '已删除',
};

const columns: AdminTableColumn<Post>[] = [
  { key: 'title', label: '帖子', minWidth: 220 },
  { key: 'author', label: '作者', minWidth: 120, formatter: (row) => row.author.displayName },
  { key: 'category', label: '分类', minWidth: 110, formatter: (row) => row.category.name },
  {
    key: 'status',
    label: '状态',
    width: 100,
    tag: (row) => ({
      label: statusLabel[row.status],
      type: row.status === 'published' ? 'success' : row.status === 'hidden' ? 'warning' : 'info',
    }),
  },
  { key: 'viewCount', label: '浏览', width: 90, align: 'right' },
  { key: 'likeCount', label: '点赞', width: 90, align: 'right' },
  { key: 'commentCount', label: '评论', width: 90, align: 'right' },
];

const actions: AdminRowAction<Post>[] = [
  {
    label: '隐藏/恢复',
    type: 'warning',
    confirmText: (row) => `确认${row.status === 'hidden' ? '恢复' : '隐藏'}《${row.title}》？`,
    successText: '帖子状态已更新',
    disabled: (row) => row.status === 'deleted',
    onConfirm: (row) => {
      row.status = row.status === 'hidden' ? 'published' : 'hidden';
      row.updatedAt = new Date().toISOString();
    },
  },
  {
    label: '标记删除',
    type: 'danger',
    confirmText: (row) => `确认将《${row.title}》标记为删除？`,
    successText: '帖子已标记删除',
    disabled: (row) => row.status === 'deleted',
    onConfirm: (row) => {
      row.status = 'deleted';
      row.updatedAt = new Date().toISOString();
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    const result = await getAdminPosts({ page: 1, pageSize: 50 });
    rows.value = result.items;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <AdminTablePage
    title="帖子管理"
    description="集中查看帖子作者、状态和互动计数，操作为本地状态演示。"
    :rows="rows"
    :columns="columns"
    :actions="actions"
    :loading="loading"
  />
</template>
