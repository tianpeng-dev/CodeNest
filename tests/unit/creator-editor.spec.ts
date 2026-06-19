import { defineComponent, h, nextTick } from 'vue';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia, type Pinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CreatorEditorPage from '@/pages/creator/CreatorEditorPage.vue';
import { useDraftStore } from '@/stores/draft.store';
import type { Post } from '@/types/post';

const { createDraftMock, publishPostMock, pushMock, successMock, updatePostMock } = vi.hoisted(() => ({
  createDraftMock: vi.fn(),
  publishPostMock: vi.fn(),
  pushMock: vi.fn(),
  successMock: vi.fn(),
  updatePostMock: vi.fn(),
}));

vi.mock('@/services/post.service', () => ({
  createDraft: createDraftMock,
  publishPost: publishPostMock,
  updatePost: updatePostMock,
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>();

  return {
    ...actual,
    ElMessage: {
      success: successMock,
      error: vi.fn(),
    },
  };
});

vi.mock('@/modules/editor/EditorShell.vue', () => ({
  default: defineComponent({
    name: 'EditorShell',
    props: {
      modelValue: { type: Object, required: true },
      mode: { type: String, required: true },
      saving: { type: Boolean, default: false },
      publishing: { type: Boolean, default: false },
      disabled: { type: Boolean, default: false },
    },
    emits: ['update:modelValue', 'update:mode', 'save', 'publish'],
    setup(props, { emit }) {
      return () =>
        h('section', { 'data-testid': 'editor-shell' }, [
          h('span', { 'data-testid': 'editor-title' }, props.modelValue.title),
          h('span', { 'data-testid': 'editor-disabled' }, String(props.disabled)),
          h(
            'button',
            {
              type: 'button',
              'data-testid': 'change-title',
              onClick: () => {
                emit('update:modelValue', {
                  ...props.modelValue,
                  title: '从测试更新的标题',
                });
              },
            },
            'change',
          ),
          h(
            'button',
            {
              type: 'button',
              'data-testid': 'save-draft',
              onClick: () => emit('save'),
            },
            'save',
          ),
          h(
            'button',
            {
              type: 'button',
              'data-testid': 'publish-post',
              onClick: () => emit('publish'),
            },
            'publish',
          ),
        ]);
    },
  }),
}));

const author = {
  id: 'user-test',
  username: 'writer',
  displayName: 'Writer',
  avatarUrl: '',
  bio: '',
  role: 'user' as const,
  status: 'active' as const,
  muteUntil: null,
  postCount: 0,
  likeCount: 0,
  favoriteCount: 0,
  followerCount: 0,
};

const category = {
  id: 'cat-frontend',
  name: '前端工程',
  slug: 'frontend',
  description: '',
  postCount: 0,
};

function postFixture(overrides: Partial<Post> = {}): Post {
  return {
    id: 'post-created',
    title: '草稿标题',
    summary: '摘要',
    content: '正文',
    coverUrl: '',
    author,
    category,
    tags: ['Vue'],
    status: 'draft',
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    commentCount: 0,
    createdAt: '2026-06-19T00:00:00.000Z',
    updatedAt: '2026-06-19T00:00:00.000Z',
    publishedAt: null,
    ...overrides,
  };
}

let pinia: Pinia;

function mountPage() {
  return mount(CreatorEditorPage, {
    global: {
      plugins: [pinia],
    },
  });
}

describe('creator editor page', () => {
  beforeEach(() => {
    createDraftMock.mockReset();
    publishPostMock.mockReset();
    updatePostMock.mockReset();
    pushMock.mockReset();
    successMock.mockReset();
    pinia = createPinia();
    setActivePinia(pinia);
  });

  it('saves the current draft through the post service and keeps the page open', async () => {
    createDraftMock.mockResolvedValueOnce(postFixture());
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '草稿标题');
    draftStore.setField('summary', '一段摘要');
    draftStore.setField('content', '## 正文');
    draftStore.setField('coverUrl', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085');
    draftStore.setField('categoryId', 'cat-frontend');
    draftStore.setField('tags', ['Vue', 'Pinia']);
    await nextTick();

    await wrapper.find('[data-testid="change-title"]').trigger('click');
    await wrapper.find('[data-testid="save-draft"]').trigger('click');

    expect(createDraftMock).toHaveBeenCalledWith({
      title: '从测试更新的标题',
      summary: '一段摘要',
      content: '## 正文',
      coverUrl: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085',
      categoryId: 'cat-frontend',
      tags: ['Vue', 'Pinia'],
      status: 'draft',
    });
    expect(successMock).toHaveBeenCalledWith('草稿已保存');
    expect(pushMock).not.toHaveBeenCalled();
    expect(wrapper.find('[data-testid="editor-shell"]').exists()).toBe(true);
  });

  it('creates once and updates the saved draft on later saves', async () => {
    createDraftMock.mockResolvedValueOnce(postFixture({ id: 'draft-001' }));
    updatePostMock.mockResolvedValueOnce(postFixture({ id: 'draft-001' }));
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '初版草稿');
    draftStore.setField('summary', '初版摘要');
    draftStore.setField('content', '初版正文');
    draftStore.setField('categoryId', 'cat-frontend');
    await nextTick();

    await wrapper.find('[data-testid="save-draft"]').trigger('click');
    draftStore.setField('summary', '更新后的摘要');
    await nextTick();
    await wrapper.find('[data-testid="save-draft"]').trigger('click');

    expect(createDraftMock).toHaveBeenCalledTimes(1);
    expect(updatePostMock).toHaveBeenCalledTimes(1);
    expect(updatePostMock).toHaveBeenCalledWith(
      'draft-001',
      expect.objectContaining({
        title: '初版草稿',
        summary: '更新后的摘要',
        status: 'draft',
      }),
    );
    expect(draftStore.currentDraftId).toBe('draft-001');
  });

  it('publishes by creating a draft, publishing it, and navigating to the post detail page', async () => {
    createDraftMock.mockResolvedValueOnce(postFixture({ id: 'post-draft' }));
    publishPostMock.mockResolvedValueOnce(
      postFixture({
        id: 'post-published',
        status: 'published',
        publishedAt: '2026-06-19T01:00:00.000Z',
      }),
    );
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '准备发布的文章');
    draftStore.setField('summary', '发布摘要');
    draftStore.setField('content', '发布正文');
    draftStore.setField('categoryId', 'cat-backend');
    await nextTick();

    await wrapper.find('[data-testid="publish-post"]').trigger('click');
    await flushPromises();

    expect(createDraftMock).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '准备发布的文章',
        status: 'draft',
      }),
    );
    expect(publishPostMock).toHaveBeenCalledWith('post-draft');
    expect(pushMock).toHaveBeenCalledWith({
      name: 'post-detail',
      params: { id: 'post-published' },
    });
  });

  it('publishes the same draft id after saving instead of creating a duplicate', async () => {
    createDraftMock.mockResolvedValueOnce(postFixture({ id: 'draft-saved' }));
    updatePostMock.mockResolvedValueOnce(postFixture({ id: 'draft-saved' }));
    publishPostMock.mockResolvedValueOnce(
      postFixture({
        id: 'draft-saved',
        status: 'published',
        publishedAt: '2026-06-19T01:00:00.000Z',
      }),
    );
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '先保存再发布');
    draftStore.setField('summary', '保存摘要');
    draftStore.setField('content', '保存正文');
    draftStore.setField('categoryId', 'cat-frontend');
    await nextTick();

    await wrapper.find('[data-testid="save-draft"]').trigger('click');
    draftStore.setField('content', '发布前更新正文');
    await nextTick();
    await wrapper.find('[data-testid="publish-post"]').trigger('click');
    await flushPromises();

    expect(createDraftMock).toHaveBeenCalledTimes(1);
    expect(updatePostMock).toHaveBeenCalledWith(
      'draft-saved',
      expect.objectContaining({
        content: '发布前更新正文',
        status: 'draft',
      }),
    );
    expect(publishPostMock).toHaveBeenCalledWith('draft-saved');
    expect(draftStore.currentDraftId).toBeNull();
  });

  it('waits for an overlapping save before publishing the same draft id', async () => {
    let resolveCreate: (post: Post) => void = () => {};
    createDraftMock.mockImplementationOnce(() => {
      return new Promise<Post>((resolve) => {
        resolveCreate = resolve;
      });
    });
    createDraftMock.mockResolvedValueOnce(postFixture({ id: 'duplicate-draft' }));
    updatePostMock.mockResolvedValueOnce(postFixture({ id: 'draft-overlap' }));
    publishPostMock.mockResolvedValueOnce(
      postFixture({
        id: 'draft-overlap',
        status: 'published',
        publishedAt: '2026-06-19T01:00:00.000Z',
      }),
    );
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '并发保存发布');
    draftStore.setField('summary', '并发摘要');
    draftStore.setField('content', '并发正文');
    draftStore.setField('categoryId', 'cat-frontend');
    await nextTick();

    const saveTrigger = wrapper.find('[data-testid="save-draft"]').trigger('click');
    await nextTick();
    const publishTrigger = wrapper.find('[data-testid="publish-post"]').trigger('click');
    await nextTick();

    expect(createDraftMock).toHaveBeenCalledTimes(1);

    resolveCreate(postFixture({ id: 'draft-overlap' }));
    await saveTrigger;
    await publishTrigger;
    await flushPromises();

    expect(createDraftMock).toHaveBeenCalledTimes(1);
    expect(updatePostMock).toHaveBeenCalledWith(
      'draft-overlap',
      expect.objectContaining({
        title: '并发保存发布',
        status: 'draft',
      }),
    );
    expect(publishPostMock).toHaveBeenCalledWith('draft-overlap');
    expect(pushMock).toHaveBeenCalledWith({
      name: 'post-detail',
      params: { id: 'draft-overlap' },
    });
  });

  it('marks editor actions disabled while persistence is in flight', async () => {
    let resolveCreate: (post: Post) => void = () => {};
    createDraftMock.mockImplementationOnce(() => {
      return new Promise<Post>((resolve) => {
        resolveCreate = resolve;
      });
    });
    const wrapper = mountPage();
    const draftStore = useDraftStore();

    draftStore.setField('title', '禁用状态草稿');
    draftStore.setField('summary', '禁用摘要');
    draftStore.setField('content', '禁用正文');
    draftStore.setField('categoryId', 'cat-frontend');
    await nextTick();

    const saveTrigger = wrapper.find('[data-testid="save-draft"]').trigger('click');
    await nextTick();

    expect(wrapper.find('[data-testid="editor-disabled"]').text()).toBe('true');

    resolveCreate(postFixture({ id: 'draft-disabled' }));
    await saveTrigger;
    await flushPromises();

    expect(wrapper.find('[data-testid="editor-disabled"]').text()).toBe('false');
  });
});
