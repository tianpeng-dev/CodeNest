import { defineStore } from 'pinia';
import type { PostDraftPayload } from '@/types/post';

export type EditorMode = 'markdown' | 'richText';

type DraftFields = PostDraftPayload;

const emptyDraft = (): DraftFields => ({
  title: '',
  summary: '',
  content: '',
  coverUrl: '',
  categoryId: '',
  tags: [],
  status: 'draft',
});

export const useDraftStore = defineStore('draft', {
  state: () => ({
    draft: emptyDraft(),
    editingMode: 'markdown' as EditorMode,
  }),
  actions: {
    setField<K extends keyof DraftFields>(field: K, value: DraftFields[K]) {
      this.draft[field] = value;
    },
    setEditingMode(mode: EditorMode) {
      this.editingMode = mode;
    },
    resetDraft() {
      this.draft = emptyDraft();
      this.editingMode = 'markdown';
    },
  },
});
