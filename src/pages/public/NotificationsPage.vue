<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Bell, Check, Refresh } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import {
  getNotifications,
  markNotificationRead,
} from '@/services/notification.service';
import type { NotificationItem } from '@/types/notification';

const notifications = ref<NotificationItem[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const pendingId = ref('');

const unreadCount = computed(() => {
  return notifications.value.filter((notification) => !notification.readAt).length;
});

const formatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

async function loadNotifications() {
  loading.value = true;
  errorMessage.value = '';

  try {
    notifications.value = await getNotifications();
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '通知列表加载失败';
    notifications.value = [];
  } finally {
    loading.value = false;
  }
}

async function markRead(notification: NotificationItem) {
  if (notification.readAt) return;
  pendingId.value = notification.id;

  try {
    const nextNotification = await markNotificationRead(notification.id);
    notifications.value = notifications.value.map((item) => {
      return item.id === nextNotification.id ? nextNotification : item;
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '标记已读失败');
  } finally {
    pendingId.value = '';
  }
}

async function markAllRead() {
  const unread = notifications.value.filter((notification) => !notification.readAt);
  for (const notification of unread) {
    await markRead(notification);
  }
}

function formatTime(value: string) {
  return formatter.format(new Date(value));
}

loadNotifications();
</script>

<template>
  <div class="notifications-page">
    <section class="notifications-heading">
      <div>
        <span class="section-kicker">通知</span>
        <h1>消息通知</h1>
        <p>站内通知为非实时列表，刷新后查看最新状态。</p>
      </div>
      <div class="notifications-heading__actions">
        <el-button @click="loadNotifications">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button
          type="primary"
          :disabled="unreadCount === 0"
          @click="markAllRead"
        >
          <el-icon><Check /></el-icon>
          <span>全部已读</span>
        </el-button>
      </div>
    </section>

    <section class="notifications-list">
      <header>
        <h2>通知列表</h2>
        <span>{{ unreadCount }} 条未读</span>
      </header>

      <el-skeleton v-if="loading" :rows="7" animated />

      <EmptyState
        v-else-if="errorMessage"
        title="通知加载失败"
        :description="errorMessage"
      >
        <template #action>
          <el-button type="primary" @click="loadNotifications">重新加载</el-button>
        </template>
      </EmptyState>

      <div v-else-if="notifications.length > 0" class="notification-items">
        <article
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ 'notification-item--unread': !notification.readAt }"
        >
          <div class="notification-item__icon">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="notification-item__body">
            <header>
              <h3>{{ notification.title }}</h3>
              <time :datetime="notification.createdAt">
                {{ formatTime(notification.createdAt) }}
              </time>
            </header>
            <p>{{ notification.content }}</p>
          </div>
          <el-button
            v-if="!notification.readAt"
            type="primary"
            plain
            :loading="pendingId === notification.id"
            @click="markRead(notification)"
          >
            标为已读
          </el-button>
          <span v-else class="notification-item__read">已读</span>
        </article>
      </div>

      <EmptyState
        v-else
        title="暂无通知"
        description="评论、审核和收藏动态会显示在这里。"
      />
    </section>
  </div>
</template>

<style scoped>
.notifications-page {
  display: grid;
  gap: 18px;
}

.notifications-heading,
.notifications-list {
  padding: 20px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.notifications-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-kicker {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.notifications-heading h1,
.notifications-list h2,
.notification-item h3 {
  margin: 0;
  color: #172033;
}

.notifications-heading h1 {
  margin-top: 4px;
  font-size: 28px;
}

.notifications-heading p {
  margin: 8px 0 0;
  color: #667085;
}

.notifications-heading__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notifications-heading__actions :deep(.el-button) {
  margin-left: 0;
}

.notifications-list > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.notifications-list h2 {
  font-size: 20px;
}

.notifications-list > header span,
.notification-item time,
.notification-item__read {
  color: #667085;
}

.notification-items {
  display: grid;
  gap: 10px;
}

.notification-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.notification-item--unread {
  background: #eef5ff;
  border-color: #c9ddff;
}

.notification-item__icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #1f4f8f;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.notification-item__body {
  min-width: 0;
}

.notification-item__body header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.notification-item h3 {
  font-size: 16px;
}

.notification-item time {
  font-size: 12px;
}

.notification-item p {
  margin: 6px 0 0;
  color: #475467;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .notifications-heading,
  .notification-item {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .notifications-heading {
    display: grid;
  }

  .notification-item__icon {
    display: none;
  }
}
</style>
