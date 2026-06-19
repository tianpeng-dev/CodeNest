<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Collection, Plus } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import { getCreatorColumns, type CreatorColumn } from '@/services/creator.service';

const loading = ref(false);
const columns = ref<CreatorColumn[]>([]);

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value));
}

onMounted(async () => {
  loading.value = true;

  try {
    columns.value = await getCreatorColumns();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="creator-columns" v-loading="loading">
    <header class="page-header">
      <div>
        <p>Series</p>
        <h1>专栏管理</h1>
      </div>
      <el-button type="primary" :icon="Plus">创建专栏</el-button>
    </header>

    <div v-if="columns.length" class="columns-grid">
      <article v-for="column in columns" :key="column.id" class="column-card">
        <img :src="column.coverUrl" alt="" />
        <div class="column-card__body">
          <el-icon><Collection /></el-icon>
          <h2>{{ column.title }}</h2>
          <p>{{ column.description }}</p>
          <div>
            <span>{{ column.postCount }} 篇文章</span>
            <span>更新于 {{ formatDate(column.updatedAt) }}</span>
          </div>
        </div>
      </article>

      <article class="column-card column-card--create">
        <el-icon><Plus /></el-icon>
        <h2>创建新专栏</h2>
        <p>把连续主题整理成系列，后续可接入专栏详情与排序能力。</p>
        <el-button type="primary">开始创建</el-button>
      </article>
    </div>

    <section v-else class="surface-panel">
      <EmptyState
        title="还没有专栏"
        description="发布几篇同主题文章后，可以把它们整理为专栏。"
      />
    </section>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header p {
  margin: 0 0 6px;
  color: #3d6f8f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.page-header h1,
.column-card h2 {
  margin: 0;
  color: #172033;
}

.page-header h1 {
  font-size: 28px;
}

.columns-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.column-card,
.surface-panel {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.column-card {
  overflow: hidden;
}

.column-card img {
  width: 100%;
  height: 132px;
  object-fit: cover;
}

.column-card__body,
.column-card--create {
  padding: 16px;
}

.column-card__body > .el-icon,
.column-card--create > .el-icon {
  color: #3d6f8f;
  font-size: 22px;
}

.column-card h2 {
  margin-top: 10px;
  font-size: 18px;
}

.column-card p {
  min-height: 46px;
  margin: 10px 0 14px;
  color: #667085;
  line-height: 1.6;
}

.column-card__body div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #98a2b3;
  font-size: 13px;
}

.column-card--create {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  justify-content: center;
  min-height: 286px;
  border-style: dashed;
}

.surface-panel {
  padding: 18px;
}

@media (max-width: 1080px) {
  .columns-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .columns-grid {
    grid-template-columns: 1fr;
  }
}
</style>
