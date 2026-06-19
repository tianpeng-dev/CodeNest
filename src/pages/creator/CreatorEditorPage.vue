<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import EditorShell from '@/modules/editor/EditorShell.vue';
import { useDraftStore } from '@/stores/draft.store';
import { createDraft, publishPost } from '@/services/post.service';
import type { PostDraftPayload } from '@/types/post';

const router = useRouter();
const draftStore = useDraftStore();
const saving = ref(false);
const publishing = ref(false);

const draft = computed({
  get: () => draftStore.draft,
  set: (value: PostDraftPayload) => {
    draftStore.replaceDraft(value);
  },
});

const mode = computed({
  get: () => draftStore.editingMode,
  set: (value: typeof draftStore.editingMode) => {
    draftStore.setEditingMode(value);
  },
});

function draftPayload(status: PostDraftPayload['status']): PostDraftPayload {
  return {
    ...draftStore.draft,
    title: draftStore.draft.title.trim(),
    summary: draftStore.draft.summary.trim(),
    content: draftStore.draft.content.trim(),
    coverUrl: draftStore.draft.coverUrl.trim(),
    categoryId: draftStore.draft.categoryId || 'cat-frontend',
    tags: draftStore.draft.tags.map((tag) => tag.trim()).filter(Boolean),
    status,
  };
}

function validateDraft(payload: PostDraftPayload) {
  if (!payload.title) {
    ElMessage.error('请先填写标题');
    return false;
  }

  if (!payload.content) {
    ElMessage.error('请先填写正文');
    return false;
  }

  return true;
}

async function saveDraft() {
  const payload = draftPayload('draft');

  if (!validateDraft(payload)) {
    return;
  }

  saving.value = true;

  try {
    await createDraft(payload);
    draftStore.replaceDraft(payload);
    ElMessage.success('草稿已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '草稿保存失败');
  } finally {
    saving.value = false;
  }
}

async function publishCurrentDraft() {
  const payload = draftPayload('draft');

  if (!validateDraft(payload)) {
    return;
  }

  publishing.value = true;

  try {
    const created = await createDraft(payload);
    const published = await publishPost(created.id);
    ElMessage.success('文章已发布');
    draftStore.resetDraft();
    await router.push({
      name: 'post-detail',
      params: { id: published.id },
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败');
  } finally {
    publishing.value = false;
  }
}
</script>

<template>
  <EditorShell
    v-model="draft"
    v-model:mode="mode"
    :saving="saving"
    :publishing="publishing"
    @save="saveDraft"
    @publish="publishCurrentDraft"
  />
</template>
