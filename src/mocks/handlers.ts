import type { AxiosRequestConfig } from 'axios';
import type AxiosMockAdapter from 'axios-mock-adapter';
import type { AdminMetric } from '../types/admin';
import type { PageResult } from '../types/common';
import type { MessageItem } from '../types/message';
import type { Post, PostDraftPayload } from '../types/post';
import type { User } from '../types/user';
import {
  mockCategories,
  mockComments,
  mockMessages,
  mockNotifications,
  mockPie,
  mockPosts,
  mockSensitiveWords,
  mockThreads,
  mockTrend,
  mockUsers,
} from './data';

type ApiData = unknown;
type MockReply = [number, { code: number; message: string; data: ApiData }];
type QueryValue = string | number | boolean | string[] | undefined;
type QueryParams = Record<string, QueryValue>;
type PostAction = 'like' | 'dislike' | 'favorite';

interface MockAccount {
  username: string;
  password: string;
  user: User;
}

const nowIso = () => new Date().toISOString();
const clone = <T>(value: T): T => structuredClone(value);

let posts: Post[] = [];
let users: User[] = [];
let comments = clone(mockComments);
let notifications = clone(mockNotifications);
let threads = clone(mockThreads);
let messages = clone(mockMessages);
let followedUserIds = new Set<string>();
let likedPostIds = new Set<string>();
let dislikedPostIds = new Set<string>();
let favoritePostIds = new Set<string>();
let accounts: MockAccount[] = [];

function seedPosts(): Post[] {
  return clone([
    ...mockPosts,
    {
      ...mockPosts[0],
      id: 'post-007',
      title: 'Vite 测试环境里的 Mock API 分层',
      summary: '用 axios mock adapter 把服务层、状态层和页面层解耦。',
      content: 'Mock API 需要尽量贴近真实接口，同时保持本地开发反馈足够快。',
      tags: ['Vite', 'Mock API', '测试'],
      viewCount: 940,
      likeCount: 73,
      favoriteCount: 27,
      commentCount: 4,
      createdAt: '2026-05-25T08:30:00.000Z',
      updatedAt: '2026-05-25T09:00:00.000Z',
      publishedAt: '2026-05-25T09:00:00.000Z',
    },
  ]);
}

export function resetMockState() {
  posts = seedPosts();
  users = clone(mockUsers);
  comments = clone(mockComments);
  notifications = clone(mockNotifications);
  threads = clone(mockThreads);
  messages = clone(mockMessages);
  followedUserIds = new Set<string>();
  likedPostIds = new Set<string>();
  dislikedPostIds = new Set<string>();
  favoritePostIds = new Set<string>();
  accounts = [
    { username: 'writer', password: 'password123', user: users[1] },
    { username: 'admin', password: 'admin123', user: users[0] },
  ];
}

resetMockState();

function ok<T>(data: T): MockReply {
  return [200, { code: 0, message: 'ok', data: clone(data) }];
}

function fail(code: number, message: string): MockReply {
  return [200, { code, message, data: null }];
}

function protectedReply(config: AxiosRequestConfig): MockReply | null {
  return currentUser(config) ? null : fail(401, '请先登录');
}

function adminReply(config: AxiosRequestConfig): MockReply | null {
  const unauthorized = protectedReply(config);
  if (unauthorized) {
    return unauthorized;
  }

  return currentUser(config)?.role === 'admin' ? null : fail(403, '无权访问');
}

function readToken(config: AxiosRequestConfig): string | null {
  const authorization = config.headers?.Authorization;

  if (typeof authorization !== 'string') {
    return null;
  }

  return authorization.replace(/^Bearer\s+/i, '') || null;
}

function currentUser(config: AxiosRequestConfig): User | null {
  const token = readToken(config);
  const account = accounts.find((item) => token === `mock-token-${item.username}`);
  return account?.user ?? null;
}

function pageOf<T>(
  items: T[],
  page = 1,
  pageSize = 10,
): PageResult<T> {
  const start = (page - 1) * pageSize;

  return {
    items: items.slice(start, start + pageSize),
    total: items.length,
    page,
    pageSize,
  };
}

function params(config: AxiosRequestConfig): QueryParams {
  return (config.params ?? {}) as QueryParams;
}

function numberParam(
  value: QueryValue,
  fallback: number,
): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function stringParam(value: QueryValue): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function getPayload<T>(config: AxiosRequestConfig): T {
  return JSON.parse(config.data ?? '{}') as T;
}

function findPost(id: string): Post | undefined {
  return posts.find((post) => post.id === id);
}

function applyPostQuery(
  source: Post[],
  query: QueryParams,
  includeDeleted = false,
): Post[] {
  const keyword = stringParam(query.keyword)?.toLowerCase();
  const categorySlug = stringParam(query.categorySlug);
  const categoryId = stringParam(query.categoryId);
  const authorId = stringParam(query.authorId);
  const tag = stringParam(query.tag);
  const tags = Array.isArray(query.tags) ? query.tags : undefined;
  const status = stringParam(query.status);
  const sort = stringParam(query.sort) ?? stringParam(query.sortBy) ?? 'latest';

  const filtered = source.filter((post) => {
    const matchesDeleted = includeDeleted || post.status !== 'deleted';
    const matchesKeyword =
      !keyword ||
      post.title.toLowerCase().includes(keyword) ||
      post.summary.toLowerCase().includes(keyword) ||
      post.content.toLowerCase().includes(keyword);
    const matchesCategory =
      (!categorySlug || post.category.slug === categorySlug) &&
      (!categoryId || post.category.id === categoryId);
    const matchesAuthor = !authorId || post.author.id === authorId;
    const matchesTag =
      !tag && !tags?.length
        ? true
        : post.tags.some((postTag) => {
            return postTag === tag || tags?.includes(postTag);
          });
    const matchesStatus = !status || post.status === status;

    return (
      matchesDeleted &&
      matchesKeyword &&
      matchesCategory &&
      matchesAuthor &&
      matchesTag &&
      matchesStatus
    );
  });

  return filtered.sort((left, right) => {
    if (sort === 'popular') {
      return right.likeCount + right.favoriteCount - left.likeCount - left.favoriteCount;
    }

    if (sort === 'commented') {
      return right.commentCount - left.commentCount;
    }

    return (
      new Date(right.publishedAt ?? right.createdAt).getTime() -
      new Date(left.publishedAt ?? left.createdAt).getTime()
    );
  });
}

function listPosts(config: AxiosRequestConfig, source = posts, includeDeleted = false) {
  const query = params(config);
  const page = numberParam(query.page, 1);
  const pageSize = numberParam(query.pageSize, 10);
  return ok(pageOf(applyPostQuery(source, query, includeDeleted), page, pageSize));
}

function buildPost(payload: PostDraftPayload, author: User): Post {
  const category =
    mockCategories.find((item) => item.id === payload.categoryId) ?? mockCategories[0];
  const timestamp = nowIso();

  return {
    id: `post-${String(posts.length + 1).padStart(3, '0')}`,
    title: payload.title,
    summary: payload.summary,
    content: payload.content,
    coverUrl: payload.coverUrl,
    author,
    category,
    tags: payload.tags,
    status: payload.status,
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    commentCount: 0,
    createdAt: timestamp,
    updatedAt: timestamp,
    publishedAt: payload.status === 'published' ? timestamp : null,
  };
}

function updatePostFromPayload(post: Post, payload: PostDraftPayload): Post {
  const category =
    mockCategories.find((item) => item.id === payload.categoryId) ?? post.category;
  const timestamp = nowIso();

  post.title = payload.title;
  post.summary = payload.summary;
  post.content = payload.content;
  post.coverUrl = payload.coverUrl;
  post.category = category;
  post.tags = payload.tags;
  post.status = payload.status;
  post.updatedAt = timestamp;
  post.publishedAt =
    payload.status === 'published' ? post.publishedAt ?? timestamp : null;

  return post;
}

function postActionReply(id: string, action: PostAction): MockReply {
  const post = findPost(id);

  if (!post) {
    return fail(404, '帖子不存在');
  }

  if (action === 'like') {
    if (likedPostIds.has(id)) {
      likedPostIds.delete(id);
      post.likeCount = Math.max(0, post.likeCount - 1);
    } else {
      likedPostIds.add(id);
      dislikedPostIds.delete(id);
      post.likeCount += 1;
    }
  }

  if (action === 'dislike') {
    if (dislikedPostIds.has(id)) {
      dislikedPostIds.delete(id);
    } else {
      dislikedPostIds.add(id);
      if (likedPostIds.delete(id)) {
        post.likeCount = Math.max(0, post.likeCount - 1);
      }
    }
  }

  if (action === 'favorite') {
    if (favoritePostIds.has(id)) {
      favoritePostIds.delete(id);
      post.favoriteCount = Math.max(0, post.favoriteCount - 1);
    } else {
      favoritePostIds.add(id);
      post.favoriteCount += 1;
    }
  }

  post.updatedAt = nowIso();
  return ok(post);
}

function adminMetrics(): AdminMetric[] {
  const activeUsers = users.filter((user) => user.status === 'active').length;
  const publishedPosts = posts.filter((post) => post.status === 'published').length;
  const commentsTotal = comments.length;

  return [
    { label: '活跃用户', value: activeUsers, trend: 8 },
    { label: '已发布帖子', value: publishedPosts, trend: 12 },
    { label: '评论总数', value: commentsTotal, trend: 5 },
    { label: '敏感词命中', value: mockSensitiveWords.length, trend: -2 },
  ];
}

export function registerMockHandlers(mock: AxiosMockAdapter) {
  mock.onPost('/auth/login').reply((config) => {
    const payload = getPayload<{ username: string; password: string }>(config);
    const account = accounts.find((item) => {
      return item.username === payload.username && item.password === payload.password;
    });

    if (!account) {
      return fail(401, '用户名或密码错误');
    }

    return ok({
      token: `mock-token-${account.username}`,
      user: account.user,
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    });
  });

  mock.onPost('/auth/register').reply((config) => {
    const payload = getPayload<{
      username: string;
      password: string;
      displayName: string;
    }>(config);
    const usernameExists =
      accounts.some((account) => account.username === payload.username) ||
      users.some((user) => user.username === payload.username);

    if (usernameExists) {
      return fail(409, '用户名已存在');
    }

    const user: User = {
      id: `user-${String(users.length + 1).padStart(3, '0')}`,
      username: payload.username,
      displayName: payload.displayName,
      avatarUrl: `https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(payload.displayName)}`,
      bio: '新加入 CodeNest 的创作者。',
      role: 'user',
      status: 'active',
      muteUntil: null,
      postCount: 0,
      likeCount: 0,
      favoriteCount: 0,
      followerCount: 0,
    };

    users.push(user);
    accounts.push({ username: payload.username, password: payload.password, user });

    return ok({
      token: `mock-token-${payload.username}`,
      user,
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    });
  });

  mock.onPost('/auth/logout').reply((config) => {
    const unauthorized = protectedReply(config);
    return unauthorized ?? ok(null);
  });

  mock.onGet('/auth/me').reply((config) => {
    const unauthorized = protectedReply(config);
    const user = currentUser(config);
    return unauthorized ?? ok(user);
  });

  mock.onGet('/posts').reply((config) => listPosts(config));

  mock.onPost('/posts/drafts').reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const author = currentUser(config);
    if (!author) return fail(401, '请先登录');

    const post = buildPost(getPayload<PostDraftPayload>(config), author);
    posts.unshift(post);
    return ok(post);
  });

  mock.onGet(/\/posts\/[^/]+$/).reply((config) => {
    const id = config.url?.split('/').at(-1) ?? '';
    const post = findPost(id);

    if (!post || post.status === 'deleted') {
      return fail(404, '帖子不存在');
    }

    return ok(post);
  });

  mock.onPost('/posts').reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const author = currentUser(config);
    if (!author) return fail(401, '请先登录');

    const post = buildPost(getPayload<PostDraftPayload>(config), author);
    posts.unshift(post);
    return ok(post);
  });

  mock.onPut(/\/posts\/[^/]+$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const id = config.url?.split('/').at(-1) ?? '';
    const post = findPost(id);

    if (!post) {
      return fail(404, '帖子不存在');
    }

    return ok(updatePostFromPayload(post, getPayload<PostDraftPayload>(config)));
  });

  mock.onDelete(/\/posts\/[^/]+$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const id = config.url?.split('/').at(-1) ?? '';
    const post = findPost(id);

    if (!post) {
      return fail(404, '帖子不存在');
    }

    post.status = 'deleted';
    post.updatedAt = nowIso();
    return ok(post);
  });

  mock.onPost(/\/posts\/[^/]+\/publish$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, id] = config.url?.match(/\/posts\/([^/]+)\/publish$/) ?? [];
    const post = id ? findPost(id) : undefined;

    if (!post) {
      return fail(404, '帖子不存在');
    }

    post.status = 'published';
    post.publishedAt = post.publishedAt ?? nowIso();
    post.updatedAt = nowIso();
    return ok(post);
  });

  mock.onPost(/\/posts\/[^/]+\/actions\/(like|dislike|favorite)$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, id, action] =
      config.url?.match(/\/posts\/([^/]+)\/actions\/(like|dislike|favorite)$/) ?? [];

    if (!id || !action) {
      return fail(404, '帖子不存在');
    }

    return postActionReply(id, action as 'like' | 'dislike' | 'favorite');
  });

  mock.onPost(/\/posts\/[^/]+\/(like|dislike|favorite)$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, id, action] =
      config.url?.match(/\/posts\/([^/]+)\/(like|dislike|favorite)$/) ?? [];

    if (!id || !action) {
      return fail(404, '帖子不存在');
    }

    return postActionReply(id, action as 'like' | 'dislike' | 'favorite');
  });

  mock.onGet(/\/posts\/[^/]+\/comments$/).reply((config) => {
    const [, postId] = config.url?.match(/\/posts\/([^/]+)\/comments$/) ?? [];
    return ok(comments.filter((comment) => comment.postId === postId));
  });

  mock.onPost(/\/posts\/[^/]+\/comments$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, postId] = config.url?.match(/\/posts\/([^/]+)\/comments$/) ?? [];
    const post = postId ? findPost(postId) : undefined;

    if (!post) {
      return fail(404, '帖子不存在');
    }

    const payload = getPayload<{ content: string }>(config);
    const author = currentUser(config);
    if (!author) return fail(401, '请先登录');

    const comment = {
      id: `comment-${String(comments.length + 1).padStart(3, '0')}`,
      postId,
      author,
      content: payload.content,
      createdAt: nowIso(),
    };

    comments.unshift(comment);
    post.commentCount += 1;
    return ok(comment);
  });

  mock.onDelete(/\/comments\/[^/]+$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const id = config.url?.split('/').at(-1) ?? '';
    const index = comments.findIndex((comment) => comment.id === id);

    if (index === -1) {
      return fail(404, '评论不存在');
    }

    const [comment] = comments.splice(index, 1);
    const post = findPost(comment.postId);

    if (post) {
      post.commentCount = Math.max(0, post.commentCount - 1);
    }

    return ok(comment);
  });

  mock.onGet(/\/users\/[^/]+$/).reply((config) => {
    const id = config.url?.split('/').at(-1) ?? '';
    const user = users.find((item) => item.id === id || item.username === id);
    return user ? ok(user) : fail(404, '用户不存在');
  });

  mock.onPost(/\/users\/[^/]+\/follow$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, id] = config.url?.match(/\/users\/([^/]+)\/follow$/) ?? [];
    const user = users.find((item) => item.id === id || item.username === id);

    if (!user) {
      return fail(404, '用户不存在');
    }

    if (followedUserIds.has(user.id)) {
      followedUserIds.delete(user.id);
      user.followerCount = Math.max(0, user.followerCount - 1);
    } else {
      followedUserIds.add(user.id);
      user.followerCount += 1;
    }

    return ok(user);
  });

  mock.onGet('/notifications').reply((config) => {
    const unauthorized = protectedReply(config);
    return unauthorized ?? ok(notifications);
  });

  mock.onPost(/\/notifications\/[^/]+\/read$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const [, id] = config.url?.match(/\/notifications\/([^/]+)\/read$/) ?? [];
    const notification = notifications.find((item) => item.id === id);

    if (!notification) {
      return fail(404, '通知不存在');
    }

    notification.readAt = notification.readAt ?? nowIso();
    return ok(notification);
  });

  mock.onGet('/messages/threads').reply((config) => {
    const unauthorized = protectedReply(config);
    return unauthorized ?? ok(threads);
  });

  mock.onGet(/\/messages\/threads\/[^/]+$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const threadId = config.url?.split('/').at(-1) ?? '';
    return ok(messages.filter((message) => message.threadId === threadId));
  });

  mock.onPost(/\/messages\/threads\/[^/]+$/).reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const threadId = config.url?.split('/').at(-1) ?? '';
    const thread = threads.find((item) => item.id === threadId);

    if (!thread) {
      return fail(404, '会话不存在');
    }

    const payload = getPayload<{ content: string }>(config);
    const sender = currentUser(config);
    if (!sender) return fail(401, '请先登录');

    const message: MessageItem = {
      id: `message-${String(messages.length + 1).padStart(3, '0')}`,
      threadId,
      sender,
      content: payload.content,
      readAt: null,
      createdAt: nowIso(),
    };

    messages.push(message);
    thread.lastMessage = payload.content;
    thread.updatedAt = message.createdAt;
    return ok(message);
  });

  mock.onGet('/creator/posts').reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const author = currentUser(config);
    if (!author) return fail(401, '请先登录');

    return listPosts(
      config,
      posts.filter((post) => post.author.id === author.id),
      true,
    );
  });

  mock.onGet('/creator/analytics').reply((config) => {
    const unauthorized = protectedReply(config);
    if (unauthorized) return unauthorized;

    const author = currentUser(config);
    if (!author) return fail(401, '请先登录');

    const creatorPosts = posts.filter((post) => post.author.id === author.id);

    return ok({
      postCount: creatorPosts.length,
      publishedCount: creatorPosts.filter((post) => post.status === 'published').length,
      draftCount: creatorPosts.filter((post) => post.status === 'draft').length,
      totalViews: creatorPosts.reduce((total, post) => total + post.viewCount, 0),
      totalLikes: creatorPosts.reduce((total, post) => total + post.likeCount, 0),
      totalFavorites: creatorPosts.reduce((total, post) => total + post.favoriteCount, 0),
      trend: mockTrend,
      pie: mockPie,
    });
  });

  mock.onGet('/admin/metrics').reply((config) => {
    const forbidden = adminReply(config);
    return forbidden ?? ok(adminMetrics());
  });

  mock.onGet('/admin/users').reply((config) => {
    const forbidden = adminReply(config);
    return forbidden ?? ok(users);
  });

  mock.onGet('/admin/posts').reply((config) => {
    const forbidden = adminReply(config);
    return forbidden ?? listPosts(config, posts, true);
  });

  mock.onGet('/admin/sensitive-words').reply((config) => {
    const forbidden = adminReply(config);
    return forbidden ?? ok(mockSensitiveWords);
  });

  mock.onGet('/admin/categories').reply((config) => {
    const forbidden = adminReply(config);
    return forbidden ?? ok(mockPie);
  });
}
