<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Position, Refresh } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import {
  getThreadMessages,
  getThreads,
  mockSendMessage,
} from '@/services/message.service';
import type { MessageItem, MessageThread } from '@/types/message';

const route = useRoute();
const threads = ref<MessageThread[]>([]);
const messages = ref<MessageItem[]>([]);
const selectedThreadId = ref('');
const draft = ref('');
const loadingThreads = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const errorMessage = ref('');
const messageError = ref('');

const selectedThread = computed(() => {
  return threads.value.find((thread) => thread.id === selectedThreadId.value) ?? null;
});

const formatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

async function loadThreads() {
  loadingThreads.value = true;
  errorMessage.value = '';

  try {
    threads.value = await getThreads();
    const targetUserId = String(route.query.to ?? '');
    const targetThread =
      threads.value.find((thread) => thread.participant.id === targetUserId) ??
      threads.value[0] ??
      null;
    selectedThreadId.value = targetThread?.id ?? '';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '私信列表加载失败';
    threads.value = [];
    selectedThreadId.value = '';
  } finally {
    loadingThreads.value = false;
  }
}

async function loadMessages(threadId: string) {
  if (!threadId) {
    messages.value = [];
    return;
  }

  loadingMessages.value = true;
  messageError.value = '';

  try {
    messages.value = await getThreadMessages(threadId);
  } catch (error) {
    messageError.value =
      error instanceof Error ? error.message : '会话内容加载失败';
    messages.value = [];
  } finally {
    loadingMessages.value = false;
  }
}

async function sendMessage() {
  const trimmed = draft.value.trim();
  if (!selectedThreadId.value || !trimmed) {
    ElMessage.warning('先选择会话并填写内容');
    return;
  }

  sending.value = true;

  try {
    const sent = await mockSendMessage(selectedThreadId.value, trimmed);
    messages.value = [...messages.value, sent];
    draft.value = '';
    ElMessage.success('私信已发送');
    await loadThreads();
    selectedThreadId.value = sent.threadId;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
  } finally {
    sending.value = false;
  }
}

function formatTime(value: string) {
  return formatter.format(new Date(value));
}

watch(
  selectedThreadId,
  (threadId) => {
    loadMessages(threadId);
  },
);

watch(
  () => route.query.to,
  () => {
    loadThreads();
  },
  { immediate: true },
);
</script>

<template>
  <div class="messages-page">
    <section class="messages-heading">
      <div>
        <span class="section-kicker">私信</span>
        <h1>站内私信</h1>
        <p>私信为非实时会话，发送后以本地 mock 服务确认。</p>
      </div>
      <el-button @click="loadThreads">
        <el-icon><Refresh /></el-icon>
        <span>刷新</span>
      </el-button>
    </section>

    <section class="messages-shell">
      <aside class="thread-list">
        <header>
          <h2>会话</h2>
          <span>{{ threads.length }}</span>
        </header>

        <el-skeleton v-if="loadingThreads" :rows="6" animated />

        <EmptyState
          v-else-if="errorMessage"
          title="会话加载失败"
          :description="errorMessage"
        >
          <template #action>
            <el-button type="primary" @click="loadThreads">重新加载</el-button>
          </template>
        </EmptyState>

        <div v-else-if="threads.length > 0" class="thread-list__items">
          <button
            v-for="thread in threads"
            :key="thread.id"
            type="button"
            :class="{ 'thread-card--active': thread.id === selectedThreadId }"
            class="thread-card"
            @click="selectedThreadId = thread.id"
          >
            <el-avatar :size="38" :src="thread.participant.avatarUrl">
              {{ thread.participant.displayName.slice(0, 1) }}
            </el-avatar>
            <span>
              <strong>{{ thread.participant.displayName }}</strong>
              <small>{{ thread.lastMessage }}</small>
            </span>
            <el-badge
              v-if="thread.unreadCount > 0"
              :value="thread.unreadCount"
              type="primary"
            />
          </button>
        </div>

        <EmptyState
          v-else
          title="暂无会话"
          description="从作者主页进入私信后，会话会显示在这里。"
        />
      </aside>

      <main class="message-detail">
        <header v-if="selectedThread" class="message-detail__header">
          <div>
            <h2>{{ selectedThread.participant.displayName }}</h2>
            <span>@{{ selectedThread.participant.username }}</span>
          </div>
          <RouterLink :to="`/u/${selectedThread.participant.id}`">
            查看主页
          </RouterLink>
        </header>

        <el-skeleton v-if="loadingMessages" :rows="8" animated />

        <EmptyState
          v-else-if="messageError"
          title="消息加载失败"
          :description="messageError"
        >
          <template #action>
            <el-button
              type="primary"
              @click="loadMessages(selectedThreadId)"
            >
              重新加载
            </el-button>
          </template>
        </EmptyState>

        <div v-else-if="messages.length > 0" class="message-timeline">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-bubble"
          >
            <el-avatar :size="32" :src="message.sender.avatarUrl">
              {{ message.sender.displayName.slice(0, 1) }}
            </el-avatar>
            <div>
              <header>
                <strong>{{ message.sender.displayName }}</strong>
                <time :datetime="message.createdAt">
                  {{ formatTime(message.createdAt) }}
                </time>
              </header>
              <p>{{ message.content }}</p>
            </div>
          </article>
        </div>

        <EmptyState
          v-else
          title="选择一个会话"
          description="会话内容会以只读消息流显示。"
        />

        <form class="message-compose" @submit.prevent="sendMessage">
          <el-input
            v-model="draft"
            placeholder="发送一条 mock 私信"
            :disabled="!selectedThread"
            maxlength="200"
            show-word-limit
          />
          <el-button
            type="primary"
            native-type="submit"
            :loading="sending"
            :disabled="!selectedThread"
          >
            <el-icon><Position /></el-icon>
            <span>发送</span>
          </el-button>
        </form>
      </main>
    </section>
  </div>
</template>

<style scoped>
.messages-page {
  display: grid;
  gap: 18px;
}

.messages-heading,
.messages-shell {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.messages-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
}

.section-kicker {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.messages-heading h1,
.thread-list h2,
.message-detail h2 {
  margin: 0;
  color: #172033;
}

.messages-heading h1 {
  margin-top: 4px;
  font-size: 28px;
}

.messages-heading p {
  margin: 8px 0 0;
  color: #667085;
}

.messages-shell {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  min-height: 560px;
  overflow: hidden;
}

.thread-list {
  padding: 16px;
  border-right: 1px solid #edf1f7;
}

.thread-list header,
.message-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.thread-list header span,
.message-detail__header span,
.message-bubble time {
  color: #667085;
}

.thread-list__items {
  display: grid;
  gap: 8px;
}

.thread-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 10px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.thread-card:hover,
.thread-card--active {
  background: #eef5ff;
  border-color: #c9ddff;
}

.thread-card span {
  min-width: 0;
}

.thread-card strong,
.thread-card small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-card strong {
  color: #172033;
}

.thread-card small {
  margin-top: 4px;
  color: #667085;
}

.message-detail {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 14px;
  min-width: 0;
  padding: 16px;
}

.message-detail__header {
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f7;
}

.message-detail__header a {
  color: #1f4f8f;
  font-weight: 800;
}

.message-timeline {
  display: grid;
  align-content: start;
  gap: 12px;
  overflow-y: auto;
  min-height: 320px;
}

.message-bubble {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
}

.message-bubble header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.message-bubble strong {
  color: #172033;
}

.message-bubble time {
  font-size: 12px;
}

.message-bubble p {
  margin: 6px 0 0;
  padding: 10px 12px;
  color: #344054;
  line-height: 1.6;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.message-compose {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #edf1f7;
}

@media (max-width: 860px) {
  .messages-shell {
    grid-template-columns: 1fr;
  }

  .thread-list {
    border-right: 0;
    border-bottom: 1px solid #edf1f7;
  }
}

@media (max-width: 560px) {
  .messages-heading,
  .message-compose {
    grid-template-columns: 1fr;
  }

  .messages-heading {
    display: grid;
  }
}
</style>
