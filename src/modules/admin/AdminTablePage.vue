<script setup lang="ts" generic="Row extends object">
import { computed, ref, watch } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

export interface AdminTableColumn<Row extends object = Record<string, unknown>> {
  key: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  align?: 'left' | 'center' | 'right';
  formatter?: (row: Row) => string | number;
  tag?: (row: Row) => {
    label: string;
    type?: 'primary' | 'success' | 'info' | 'warning' | 'danger';
  };
}

export interface AdminRowAction<Row extends object = Record<string, unknown>> {
  label: string;
  type?: 'primary' | 'success' | 'info' | 'warning' | 'danger';
  confirmText?: string | ((row: Row) => string);
  successText?: string | ((row: Row) => string);
  disabled?: (row: Row) => boolean;
  onConfirm: (row: Row) => void;
}

const props = withDefaults(
  defineProps<{
    title: string;
    description: string;
    rows: Row[];
    columns: AdminTableColumn<Row>[];
    actions?: AdminRowAction<Row>[];
    pageSize?: number;
    loading?: boolean;
  }>(),
  {
    actions: () => [],
    pageSize: 8,
    loading: false,
  },
);

const keyword = ref('');
const page = ref(1);

const getRowText = (row: Row) => {
  return props.columns
    .map((column) => {
      const rawValue = String((row as Record<string, unknown>)[column.key] ?? '');
      const formattedValue = column.formatter ? String(column.formatter(row)) : '';
      const tagLabel = column.tag?.(row).label ?? '';

      return [tagLabel, formattedValue, rawValue].filter(Boolean).join(' ');
    })
    .join(' ')
    .toLowerCase();
};

const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return props.rows;
  return props.rows.filter((row) => getRowText(row).includes(query));
});

const pagedRows = computed(() => {
  const start = (page.value - 1) * props.pageSize;
  return filteredRows.value.slice(start, start + props.pageSize);
});

const cellValue = (row: Row, column: AdminTableColumn<Row>) => {
  if (column.formatter) return column.formatter(row);
  return (row as Record<string, unknown>)[column.key] ?? '-';
};

const tagValue = (row: Row, column: AdminTableColumn<Row>) => {
  return column.tag?.(row);
};

const actionText = (
  text: string | ((row: Row) => string) | undefined,
  row: Row,
  fallback: string,
) => {
  if (typeof text === 'function') return text(row);
  return text ?? fallback;
};

const runAction = async (action: AdminRowAction<Row>, row: Row) => {
  const confirmText = actionText(action.confirmText, row, `确认执行“${action.label}”？`);

  try {
    await ElMessageBox.confirm(confirmText, '操作确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });
    action.onConfirm(row);
    ElMessage.success(actionText(action.successText, row, '操作已完成'));
  } catch {
    // Element Plus rejects both cancel and close; no user-facing error needed.
  }
};

watch(
  () => [keyword.value, props.rows.length],
  () => {
    page.value = 1;
  },
);
</script>

<template>
  <section class="admin-table-page">
    <header class="admin-table-page__header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <el-input
        v-model="keyword"
        class="admin-table-page__filter"
        data-testid="admin-table-filter"
        clearable
        placeholder="筛选当前列表"
        :prefix-icon="Search"
      />
    </header>

    <el-table
      v-loading="loading"
      class="admin-table-page__table"
      :data="pagedRows"
      border
      stripe
      empty-text="没有匹配的数据"
    >
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align"
      >
        <template #default="{ row }">
          <el-tag v-if="tagValue(row, column)" :type="tagValue(row, column)?.type" effect="light">
            {{ tagValue(row, column)?.label }}
          </el-tag>
          <span v-else>{{ cellValue(row, column) }}</span>
        </template>
      </el-table-column>

      <el-table-column v-if="actions.length" label="操作" min-width="210">
        <template #default="{ row }">
          <div class="admin-table-page__actions">
            <el-button
              v-for="action in actions"
              :key="action.label"
              size="small"
              :type="action.type ?? 'primary'"
              :disabled="action.disabled?.(row)"
              @click="runAction(action, row)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <footer class="admin-table-page__footer">
      <span>共 {{ filteredRows.length }} 条</span>
      <el-pagination
        v-model:current-page="page"
        size="small"
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="filteredRows.length"
      />
    </footer>
  </section>
</template>

<style scoped>
.admin-table-page {
  display: grid;
  gap: 16px;
}

.admin-table-page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.admin-table-page__header h1 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.2;
}

.admin-table-page__header p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.admin-table-page__filter {
  width: 260px;
  max-width: 100%;
}

.admin-table-page__table {
  width: 100%;
}

.admin-table-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-table-page__actions :deep(.el-button) {
  margin-left: 0;
}

.admin-table-page__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 760px) {
  .admin-table-page__header,
  .admin-table-page__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-table-page__filter {
    width: 100%;
  }
}
</style>
