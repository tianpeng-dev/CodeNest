<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Cpu, Document, EditPen, MagicStick } from '@element-plus/icons-vue';
import { MdEditor, MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import type { EditorMode } from '@/stores/draft.store';
import type { PostDraftPayload } from '@/types/post';

const props = defineProps<{
  modelValue: PostDraftPayload;
  mode: EditorMode;
  saving?: boolean;
  publishing?: boolean;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: PostDraftPayload): void;
  (event: 'update:mode', value: EditorMode): void;
  (event: 'save'): void;
  (event: 'publish'): void;
}>();

type EditorTab = EditorMode | 'preview';

const categoryOptions = [
  { label: '前端工程', value: 'cat-frontend' },
  { label: '后端架构', value: 'cat-backend' },
  { label: '社区运营', value: 'cat-growth' },
];

const coverPresets = [
  {
    label: '代码工作台',
    value: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085',
  },
  {
    label: '工程屏幕',
    value: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4',
  },
  {
    label: '数据仪表',
    value: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71',
  },
];

const activeTab = ref<EditorTab>(props.mode);
const tagInput = ref('');
const aiDialogVisible = ref(false);
const aiDialogType = ref<'topic' | 'article'>('topic');

const wordCount = computed(() => props.modelValue.content.trim().length);
const selectedCoverLabel = computed(() => {
  return coverPresets.find((item) => item.value === props.modelValue.coverUrl)?.label;
});

function updateField<K extends keyof PostDraftPayload>(
  field: K,
  value: PostDraftPayload[K],
) {
  emit('update:modelValue', {
    ...props.modelValue,
    [field]: value,
  });
}

function switchTab(tab: string | number) {
  const nextTab = String(tab) as EditorTab;
  activeTab.value = nextTab;

  if (nextTab === 'markdown' || nextTab === 'richText') {
    emit('update:mode', nextTab);
  }
}

function addTag() {
  const normalized = tagInput.value.trim();

  if (!normalized) {
    return;
  }

  if (props.modelValue.tags.includes(normalized)) {
    ElMessage.error('标签已存在');
    return;
  }

  updateField('tags', [...props.modelValue.tags, normalized]);
  tagInput.value = '';
}

function removeTag(tag: string) {
  updateField(
    'tags',
    props.modelValue.tags.filter((item) => item !== tag),
  );
}

function openAiDialog(type: 'topic' | 'article') {
  aiDialogType.value = type;
  aiDialogVisible.value = true;
}
</script>

<template>
  <section class="editor-shell">
    <header class="editor-shell__header">
      <div>
        <p class="editor-shell__eyebrow">Creator Studio</p>
        <h1>写一篇新文章</h1>
      </div>
      <div class="editor-shell__actions">
        <el-button :icon="MagicStick" @click="openAiDialog('topic')">
          AI 选题
        </el-button>
        <el-button :icon="Cpu" @click="openAiDialog('article')">
          AI 成文
        </el-button>
        <el-button :loading="saving" @click="$emit('save')">保存草稿</el-button>
        <el-button type="primary" :loading="publishing" @click="$emit('publish')">
          发布
        </el-button>
      </div>
    </header>

    <div class="editor-shell__grid">
      <section class="editor-shell__main">
        <el-input
          class="editor-shell__title"
          :model-value="modelValue.title"
          placeholder="输入文章标题"
          maxlength="80"
          show-word-limit
          @update:model-value="updateField('title', String($event))"
        />

        <el-tabs :model-value="activeTab" class="editor-shell__tabs" @tab-change="switchTab">
          <el-tab-pane name="markdown">
            <template #label>
              <span class="editor-shell__tab-label">
                <el-icon><Document /></el-icon>
                Markdown
              </span>
            </template>
            <MdEditor
              :model-value="modelValue.content"
              language="zh-CN"
              preview-theme="github"
              code-theme="github"
              :toolbars-exclude="['github']"
              @update:model-value="updateField('content', String($event))"
            />
          </el-tab-pane>

          <el-tab-pane name="richText">
            <template #label>
              <span class="editor-shell__tab-label">
                <el-icon><EditPen /></el-icon>
                富文本
              </span>
            </template>
            <el-input
              :model-value="modelValue.content"
              type="textarea"
              resize="none"
              class="editor-shell__rich-text"
              placeholder="在这里用更轻量的方式写作，内容会同步保存为 Markdown。"
              @update:model-value="updateField('content', String($event))"
            />
          </el-tab-pane>

          <el-tab-pane label="预览" name="preview">
            <div class="editor-shell__preview">
              <MdPreview
                editor-id="creator-editor-preview"
                :model-value="modelValue.content || '开始写作后，这里会显示预览。'"
                preview-theme="github"
                code-theme="github"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="editor-shell__side">
        <section class="editor-panel">
          <h2>发布信息</h2>
          <el-form label-position="top">
            <el-form-item label="分类">
              <el-select
                :model-value="modelValue.categoryId"
                placeholder="选择分类"
                @update:model-value="updateField('categoryId', String($event))"
              >
                <el-option
                  v-for="category in categoryOptions"
                  :key="category.value"
                  :label="category.label"
                  :value="category.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="摘要">
              <el-input
                :model-value="modelValue.summary"
                type="textarea"
                :rows="4"
                maxlength="160"
                show-word-limit
                placeholder="用一句清楚的话告诉读者这篇文章解决什么问题。"
                @update:model-value="updateField('summary', String($event))"
              />
            </el-form-item>

            <el-form-item label="封面">
              <el-select
                :model-value="selectedCoverLabel ? modelValue.coverUrl : ''"
                placeholder="选择预设封面"
                clearable
                @update:model-value="updateField('coverUrl', String($event))"
              >
                <el-option
                  v-for="cover in coverPresets"
                  :key="cover.value"
                  :label="cover.label"
                  :value="cover.value"
                />
              </el-select>
              <el-input
                class="editor-shell__cover-input"
                :model-value="modelValue.coverUrl"
                placeholder="或粘贴封面 URL"
                @update:model-value="updateField('coverUrl', String($event))"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="editor-panel">
          <h2>标签</h2>
          <div class="editor-shell__tag-input">
            <el-input
              v-model="tagInput"
              placeholder="输入标签"
              @keyup.enter="addTag"
            />
            <el-button @click="addTag">添加</el-button>
          </div>
          <div class="editor-shell__tags">
            <el-tag
              v-for="tag in modelValue.tags"
              :key="tag"
              closable
              @close="removeTag(tag)"
            >
              {{ tag }}
            </el-tag>
            <span v-if="modelValue.tags.length === 0" class="editor-shell__muted">
              还没有标签
            </span>
          </div>
        </section>

        <section class="editor-panel editor-panel--metrics">
          <span>正文字符</span>
          <strong>{{ wordCount }}</strong>
        </section>
      </aside>
    </div>

    <el-dialog v-model="aiDialogVisible" width="420px">
      <template #header>
        {{ aiDialogType === 'topic' ? 'AI 选题助手' : 'AI 成文助手' }}
      </template>
      <p class="editor-shell__dialog-copy">
        这里预留给后续 AI 创作能力集成。当前版本不会生成或改写内容，避免误导创作者。
      </p>
      <template #footer>
        <el-button type="primary" @click="aiDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.editor-shell {
  min-width: 0;
}

.editor-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.editor-shell__eyebrow {
  margin: 0 0 6px;
  color: #3d6f8f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.editor-shell__header h1 {
  margin: 0;
  color: #172033;
  font-size: 28px;
  line-height: 1.2;
}

.editor-shell__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.editor-shell__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.editor-shell__main,
.editor-panel {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.editor-shell__main {
  min-width: 0;
  padding: 18px;
}

.editor-shell__title {
  margin-bottom: 14px;
}

.editor-shell__title :deep(.el-input__wrapper) {
  min-height: 48px;
  font-size: 20px;
  font-weight: 800;
}

.editor-shell__tabs :deep(.md-editor) {
  height: min(62vh, 680px);
  min-height: 420px;
  border-radius: 8px;
}

.editor-shell__tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.editor-shell__rich-text :deep(.el-textarea__inner) {
  min-height: 420px !important;
  padding: 18px;
  color: #172033;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.8;
}

.editor-shell__preview {
  min-height: 420px;
  padding: 16px;
  background: #fbfcfe;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.editor-shell__side {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
}

.editor-panel {
  padding: 16px;
}

.editor-panel h2 {
  margin: 0 0 14px;
  color: #172033;
  font-size: 16px;
}

.editor-shell__cover-input,
.editor-shell__tag-input {
  margin-top: 10px;
}

.editor-shell__tag-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.editor-shell__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  margin-top: 12px;
}

.editor-shell__muted {
  color: #98a2b3;
  font-size: 13px;
}

.editor-panel--metrics {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #667085;
}

.editor-panel--metrics strong {
  color: #172033;
  font-size: 24px;
}

.editor-shell__dialog-copy {
  margin: 0;
  color: #475467;
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .editor-shell__grid {
    grid-template-columns: 1fr;
  }

  .editor-shell__side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .editor-shell__header {
    display: block;
  }

  .editor-shell__actions {
    justify-content: flex-start;
    margin-top: 14px;
  }

  .editor-shell__side {
    grid-template-columns: 1fr;
  }

  .editor-shell__tabs :deep(.md-editor),
  .editor-shell__rich-text :deep(.el-textarea__inner),
  .editor-shell__preview {
    min-height: 360px;
  }
}
</style>
