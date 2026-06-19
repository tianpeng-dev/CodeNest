# CodeNest Frontend V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Vue 3 frontend-only technical blog and forum prototype covering the public site, creator center, and admin console with Mock API data and local interaction loops.

**Architecture:** The app uses three route layouts (`PublicLayout`, `UserCenterLayout`, `AdminLayout`) with route guards for guest, user, and admin access. UI code calls `services/*`, services call Axios, and Axios is intercepted by a Mock API layer so later Spring Boot integration can replace the service transport without rewriting pages.

**Tech Stack:** Vue 3, TypeScript, Vite, Element Plus, Vue Router, Pinia, Axios, axios-mock-adapter, ECharts, md-editor-v3, DOMPurify, Vitest, Vue Test Utils, Playwright.

---

## Spec Source

Implement from:

- `docs/superpowers/specs/2026-06-19-codenest-frontend-v1-design.md`

## File Structure To Create

```text
package.json
index.html
vite.config.ts
vitest.config.ts
tsconfig.json
tsconfig.node.json
playwright.config.ts
src/
  main.ts
  App.vue
  app/
    router.ts
    permissions.ts
    head.ts
  assets/
  components/
    EmptyState.vue
    ForbiddenState.vue
    StatCard.vue
  layouts/
    PublicLayout.vue
    UserCenterLayout.vue
    AdminLayout.vue
  modules/
    admin/
      AdminTablePage.vue
    analytics/
      StatsChart.vue
    comment/
      CommentList.vue
    editor/
      EditorShell.vue
    post/
      AuthorSidebar.vue
      InteractionBar.vue
      PostCard.vue
      PostList.vue
    user/
      UserSummaryCard.vue
  pages/
    admin/
      AdminAnalyticsPage.vue
      AdminCategoriesPage.vue
      AdminDashboardPage.vue
      AdminModeratorsPage.vue
      AdminPostsPage.vue
      AdminSensitiveWordsPage.vue
      AdminUsersPage.vue
    auth/
      LoginPage.vue
      RegisterPage.vue
    creator/
      CreatorAnalyticsPage.vue
      CreatorColumnsPage.vue
      CreatorCommentsPage.vue
      CreatorEditorPage.vue
      CreatorOverviewPage.vue
      CreatorPostsPage.vue
    errors/
      ForbiddenPage.vue
      NotFoundPage.vue
    public/
      CategoryPage.vue
      HomePage.vue
      MessagesPage.vue
      NotificationsPage.vue
      PostDetailPage.vue
      SearchPage.vue
      UserProfilePage.vue
  services/
    admin.service.ts
    auth.service.ts
    comment.service.ts
    creator.service.ts
    http.ts
    message.service.ts
    notification.service.ts
    post.service.ts
    user.service.ts
  mocks/
    data.ts
    handlers.ts
    mock.ts
  stores/
    auth.store.ts
    draft.store.ts
    interaction.store.ts
    ui.store.ts
  styles/
    base.css
    element.css
  types/
    admin.ts
    analytics.ts
    auth.ts
    comment.ts
    common.ts
    message.ts
    notification.ts
    post.ts
    user.ts
  utils/
    date.ts
    markdown.ts
tests/
  unit/
    auth.store.spec.ts
    permissions.spec.ts
    post.service.spec.ts
  e2e/
    smoke.spec.ts
netlify.toml
```

## Implementation Tasks

### Task 1: Scaffold The Vue App

**Files:**
- Create generated Vite app files in project root.
- Modify: `.gitignore`
- Create: `netlify.toml`

- [ ] **Step 1: Scaffold Vite Vue TypeScript into a temporary directory**

Run:

```bash
npm create vite@latest /tmp/codenest-vite -- --template vue-ts
```

Expected: command completes and `/tmp/codenest-vite/package.json` exists.

- [ ] **Step 2: Copy scaffold files into the repository without overwriting docs**

Run:

```bash
cp -R /tmp/codenest-vite/. /Users/peng/Documents/Project/CodeNest/
```

Expected: `package.json`, `index.html`, `vite.config.ts`, and `src/main.ts` exist in the repository.

- [ ] **Step 3: Install dependencies**

Run:

```bash
npm install
npm install element-plus @element-plus/icons-vue vue-router pinia axios axios-mock-adapter echarts vue-echarts md-editor-v3 dompurify
npm install -D vitest @vue/test-utils jsdom playwright @playwright/test
```

Expected: `package-lock.json` is created or updated.

- [ ] **Step 4: Add Netlify SPA redirect**

Create `netlify.toml`:

```toml
[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

- [ ] **Step 5: Extend `.gitignore`**

Ensure `.gitignore` contains:

```gitignore
.superpowers/
node_modules/
dist/
.env
.env.*
!.env.example
.DS_Store
coverage/
test-results/
playwright-report/
```

- [ ] **Step 6: Verify scaffold**

Run:

```bash
npm run build
```

Expected: Vite build succeeds and creates `dist/`.

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "chore: scaffold vue frontend app"
```

### Task 2: Configure Testing, Styles, And App Bootstrap

**Files:**
- Modify: `package.json`
- Create: `vitest.config.ts`
- Create: `playwright.config.ts`
- Modify: `src/main.ts`
- Modify: `src/App.vue`
- Create: `src/styles/base.css`
- Create: `src/styles/element.css`

- [ ] **Step 1: Add scripts to `package.json`**

Set these scripts:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "test:unit": "vitest run",
    "test:e2e": "playwright test",
    "test": "npm run test:unit && npm run test:e2e"
  }
}
```

- [ ] **Step 2: Create `vitest.config.ts`**

```ts
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    css: true,
  },
});
```

- [ ] **Step 3: Create `playwright.config.ts`**

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'npm run build && npm run preview -- --host 127.0.0.1',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: false,
    timeout: 120000,
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 5'] } },
  ],
});
```

- [ ] **Step 4: Replace `src/main.ts`**

```ts
import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'md-editor-v3/lib/style.css';
import './styles/base.css';
import './styles/element.css';
import App from './App.vue';
import { router } from './app/router';
import { createPinia } from 'pinia';
import { setupMockApi } from './mocks/mock';

setupMockApi();

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
  .mount('#app');
```

- [ ] **Step 5: Replace `src/App.vue`**

```vue
<template>
  <RouterView />
</template>
```

- [ ] **Step 6: Create base styles**

`src/styles/base.css`:

```css
:root {
  color: #172033;
  background: #f5f7fb;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}

a {
  color: inherit;
  text-decoration: none;
}

.page-shell {
  width: min(1440px, calc(100vw - 32px));
  margin: 0 auto;
}

.muted {
  color: #667085;
}

@media (max-width: 768px) {
  .page-shell {
    width: calc(100vw - 20px);
  }
}
```

`src/styles/element.css`:

```css
.el-card {
  border-radius: 8px;
}

.el-button {
  font-weight: 600;
}
```

- [ ] **Step 7: Run checks**

Run:

```bash
npm run build
npx vitest run --passWithNoTests
```

Expected: build succeeds; Vitest exits successfully even before unit specs are introduced.

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "chore: configure app bootstrap and tests"
```

### Task 3: Define Domain Types And Mock Seed Data

**Files:**
- Create: `src/types/common.ts`
- Create: `src/types/auth.ts`
- Create: `src/types/user.ts`
- Create: `src/types/post.ts`
- Create: `src/types/comment.ts`
- Create: `src/types/notification.ts`
- Create: `src/types/message.ts`
- Create: `src/types/analytics.ts`
- Create: `src/types/admin.ts`
- Create: `src/mocks/data.ts`

- [ ] **Step 1: Create shared types**

`src/types/common.ts`:

```ts
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export type ID = string;
```

- [ ] **Step 2: Create auth and user types**

`src/types/auth.ts`:

```ts
import type { User } from './user';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  displayName: string;
}

export interface AuthSession {
  token: string;
  user: User;
}
```

`src/types/user.ts`:

```ts
import type { ID } from './common';

export type UserRole = 'user' | 'admin';
export type UserStatus = 'active' | 'banned';

export interface User {
  id: ID;
  username: string;
  displayName: string;
  avatarUrl: string;
  bio: string;
  role: UserRole;
  status: UserStatus;
  muteUntil: string | null;
  postCount: number;
  likeCount: number;
  favoriteCount: number;
  followerCount: number;
}
```

- [ ] **Step 3: Create content types**

`src/types/post.ts`:

```ts
import type { ID } from './common';
import type { User } from './user';

export type PostStatus = 'draft' | 'published' | 'hidden' | 'deleted';

export interface Category {
  id: ID;
  name: string;
  slug: string;
  description: string;
}

export interface Post {
  id: ID;
  title: string;
  summary: string;
  content: string;
  coverUrl: string;
  categoryId: ID;
  tags: string[];
  author: User;
  status: PostStatus;
  viewCount: number;
  likeCount: number;
  dislikeCount: number;
  favoriteCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface PostQuery {
  keyword?: string;
  categorySlug?: string;
  tag?: string;
  sort?: 'latest' | 'popular';
  page?: number;
  pageSize?: number;
}

export interface PostDraftPayload {
  title: string;
  summary: string;
  content: string;
  coverUrl: string;
  categoryId: ID;
  tags: string[];
}
```

`src/types/comment.ts`:

```ts
import type { ID } from './common';
import type { User } from './user';

export interface Comment {
  id: ID;
  postId: ID;
  author: User;
  content: string;
  createdAt: string;
}
```

- [ ] **Step 4: Create messaging, analytics, and admin types**

`src/types/notification.ts`:

```ts
import type { ID } from './common';

export interface NotificationItem {
  id: ID;
  title: string;
  content: string;
  readAt: string | null;
  createdAt: string;
}
```

`src/types/message.ts`:

```ts
import type { ID } from './common';

export interface MessageThread {
  id: ID;
  participantName: string;
  avatarUrl: string;
  lastMessage: string;
  updatedAt: string;
  unreadCount: number;
}

export interface MessageItem {
  id: ID;
  threadId: ID;
  senderName: string;
  content: string;
  createdAt: string;
}
```

`src/types/analytics.ts`:

```ts
export interface TrendPoint {
  date: string;
  views: number;
  likes: number;
  favorites: number;
}

export interface PiePoint {
  name: string;
  value: number;
}
```

`src/types/admin.ts`:

```ts
export interface AdminMetric {
  label: string;
  value: number;
  delta: string;
}

export interface SensitiveWord {
  id: string;
  word: string;
  level: 'low' | 'medium' | 'high';
  createdAt: string;
}
```

- [ ] **Step 5: Create mock seed data**

`src/mocks/data.ts` must export arrays named `mockUsers`, `mockCategories`, `mockPosts`, `mockComments`, `mockNotifications`, `mockThreads`, `mockMessages`, `mockTrend`, `mockPie`, and `mockSensitiveWords`. Use at least two users, six posts, three categories, three comments, three notifications, two message threads, and three sensitive words. Make one user `admin` and one user `user`.

- [ ] **Step 6: Run type check**

Run:

```bash
npm run build
```

Expected: TypeScript build succeeds.

- [ ] **Step 7: Commit**

```bash
git add src/types src/mocks/data.ts
git commit -m "feat: define domain types and mock data"
```

### Task 4: Implement HTTP Services And Mock API

**Files:**
- Create: `src/services/http.ts`
- Create: `src/services/auth.service.ts`
- Create: `src/services/post.service.ts`
- Create: `src/services/comment.service.ts`
- Create: `src/services/user.service.ts`
- Create: `src/services/creator.service.ts`
- Create: `src/services/admin.service.ts`
- Create: `src/services/notification.service.ts`
- Create: `src/services/message.service.ts`
- Create: `src/mocks/mock.ts`
- Create: `src/mocks/handlers.ts`
- Create: `tests/unit/post.service.spec.ts`

- [ ] **Step 1: Write the service test**

`tests/unit/post.service.spec.ts`:

```ts
import { describe, expect, it, beforeAll } from 'vitest';
import { setupMockApi } from '@/mocks/mock';
import { getPosts, getPostById } from '@/services/post.service';

beforeAll(() => {
  setupMockApi();
});

describe('post service', () => {
  it('returns paginated posts', async () => {
    const result = await getPosts({ page: 1, pageSize: 3 });
    expect(result.items).toHaveLength(3);
    expect(result.total).toBeGreaterThanOrEqual(6);
  });

  it('returns one post by id', async () => {
    const list = await getPosts({ page: 1, pageSize: 1 });
    const post = await getPostById(list.items[0].id);
    expect(post.id).toBe(list.items[0].id);
  });
});
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
npm run test:unit -- tests/unit/post.service.spec.ts
```

Expected: fails because service and mock files are missing.

- [ ] **Step 3: Create Axios client**

`src/services/http.ts`:

```ts
import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('codenest_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function unwrap<T>(response: { data: { code: number; message: string; data: T } }): T {
  if (response.data.code !== 0) {
    throw new Error(response.data.message);
  }
  return response.data.data;
}
```

- [ ] **Step 4: Create post service**

`src/services/post.service.ts`:

```ts
import { http, unwrap } from './http';
import type { PageResult } from '@/types/common';
import type { Post, PostDraftPayload, PostQuery } from '@/types/post';

export async function getPosts(query: PostQuery = {}) {
  return unwrap<PageResult<Post>>(await http.get('/posts', { params: query }));
}

export async function getPostById(id: string) {
  return unwrap<Post>(await http.get(`/posts/${id}`));
}

export async function createDraft(payload: PostDraftPayload) {
  return unwrap<Post>(await http.post('/posts/drafts', payload));
}

export async function publishPost(id: string) {
  return unwrap<Post>(await http.post(`/posts/${id}/publish`));
}

export async function updatePost(id: string, payload: PostDraftPayload) {
  return unwrap<Post>(await http.put(`/posts/${id}`, payload));
}

export async function deletePost(id: string) {
  return unwrap<Post>(await http.delete(`/posts/${id}`));
}

export async function togglePostAction(id: string, action: 'like' | 'dislike' | 'favorite') {
  return unwrap<Post>(await http.post(`/posts/${id}/${action}`));
}
```

- [ ] **Step 5: Create remaining services**

Create service files with these exported functions:

```ts
// auth.service.ts
export async function login(payload: LoginPayload): Promise<AuthSession>;
export async function register(payload: RegisterPayload): Promise<AuthSession>;
export async function logout(): Promise<void>;
export async function getCurrentUser(): Promise<User>;

// comment.service.ts
export async function getComments(postId: string): Promise<Comment[]>;
export async function createComment(postId: string, content: string): Promise<Comment>;
export async function deleteComment(commentId: string): Promise<Comment>;

// user.service.ts
export async function getUserProfile(id: string): Promise<User>;
export async function toggleFollow(id: string): Promise<User>;

// notification.service.ts
export async function getNotifications(): Promise<NotificationItem[]>;
export async function markNotificationRead(id: string): Promise<NotificationItem>;

// message.service.ts
export async function getThreads(): Promise<MessageThread[]>;
export async function getThreadMessages(threadId: string): Promise<MessageItem[]>;
export async function mockSendMessage(threadId: string, content: string): Promise<MessageItem>;

// creator.service.ts
export async function getCreatorPosts(): Promise<Post[]>;
export async function getCreatorAnalytics(): Promise<{ trend: TrendPoint[]; pie: PiePoint[] }>;

// admin.service.ts
export async function getAdminMetrics(): Promise<AdminMetric[]>;
export async function getAdminUsers(): Promise<User[]>;
export async function getAdminPosts(): Promise<Post[]>;
export async function getSensitiveWords(): Promise<SensitiveWord[]>;
```

- [ ] **Step 6: Create mock adapter**

`src/mocks/mock.ts`:

```ts
import AxiosMockAdapter from 'axios-mock-adapter';
import { http } from '@/services/http';
import { registerMockHandlers } from './handlers';

let installed = false;

export function setupMockApi() {
  if (installed) return;
  installed = true;
  const mock = new AxiosMockAdapter(http, { delayResponse: 300 });
  registerMockHandlers(mock);
}
```

- [ ] **Step 7: Create mock handlers**

`src/mocks/handlers.ts` must:

- Return `{ code: 0, message: 'ok', data }` on success.
- Return `{ code: 401, message: '请先登录', data: null }` for protected endpoints without a token.
- Support `/posts`, `/posts/:id`, `/posts/:id/like`, `/posts/:id/dislike`, `/posts/:id/favorite`.
- Support auth endpoints and store no real secrets.
- Use `mockUsers`, `mockPosts`, and the other arrays from `src/mocks/data.ts`.

- [ ] **Step 8: Run tests**

Run:

```bash
npm run test:unit -- tests/unit/post.service.spec.ts
npm run build
```

Expected: test passes and build succeeds.

- [ ] **Step 9: Commit**

```bash
git add src/services src/mocks tests/unit/post.service.spec.ts
git commit -m "feat: add mock api service layer"
```

### Task 5: Implement Stores, Permissions, Router, And Layout Shells

**Files:**
- Create: `src/app/permissions.ts`
- Create: `src/app/router.ts`
- Create: `src/app/head.ts`
- Create: `src/stores/auth.store.ts`
- Create: `src/stores/interaction.store.ts`
- Create: `src/stores/draft.store.ts`
- Create: `src/stores/ui.store.ts`
- Create: `src/layouts/PublicLayout.vue`
- Create: `src/layouts/UserCenterLayout.vue`
- Create: `src/layouts/AdminLayout.vue`
- Create: `tests/unit/permissions.spec.ts`
- Create: `tests/unit/auth.store.spec.ts`

- [ ] **Step 1: Write permission test**

`tests/unit/permissions.spec.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { canAccessRoute } from '@/app/permissions';
import type { User } from '@/types/user';

const user: User = {
  id: 'u1',
  username: 'writer',
  displayName: 'Writer',
  avatarUrl: '',
  bio: '',
  role: 'user',
  status: 'active',
  muteUntil: null,
  postCount: 1,
  likeCount: 1,
  favoriteCount: 1,
  followerCount: 1,
};

const admin: User = { ...user, id: 'admin', role: 'admin' };

describe('canAccessRoute', () => {
  it('allows public routes for guests', () => {
    expect(canAccessRoute(undefined, undefined)).toBe(true);
  });

  it('blocks user routes for guests', () => {
    expect(canAccessRoute('user', undefined)).toBe(false);
  });

  it('blocks admin routes for normal users', () => {
    expect(canAccessRoute('admin', user)).toBe(false);
  });

  it('allows admin routes for admins', () => {
    expect(canAccessRoute('admin', admin)).toBe(true);
  });
});
```

- [ ] **Step 2: Implement permission helper**

`src/app/permissions.ts`:

```ts
import type { User } from '@/types/user';

export type RouteAccess = 'public' | 'user' | 'admin' | undefined;

export function canAccessRoute(access: RouteAccess, user: User | null | undefined) {
  if (!access || access === 'public') return true;
  if (!user) return false;
  if (access === 'user') return user.status === 'active';
  return user.role === 'admin' && user.status === 'active';
}
```

- [ ] **Step 3: Implement stores**

Create Pinia stores:

- `auth.store.ts`: `token`, `currentUser`, `isLoggedIn`, `isAdmin`, `login`, `register`, `logout`, `loadCurrentUser`.
- `interaction.store.ts`: maps for liked, disliked, favorited, and followed IDs with toggle helpers.
- `draft.store.ts`: current draft fields, editing mode `'markdown' | 'richText'`, `setField`, `resetDraft`.
- `ui.store.ts`: `sidebarCollapsed`, `toggleSidebar`.

- [ ] **Step 4: Implement router**

`src/app/router.ts` must:

- Use `createRouter` and `createWebHistory`.
- Register all routes from the design spec.
- Add route meta `access: 'public' | 'user' | 'admin'`.
- Redirect unauthenticated protected routes to `/login?redirect=<target>`.
- Redirect unauthorized admin access to `/403`.
- Use lazy page imports.

- [ ] **Step 5: Create layout shells**

`PublicLayout.vue`, `UserCenterLayout.vue`, and `AdminLayout.vue` must each render a navigation shell and `<RouterView />`. Use Element Plus menu components and stable CSS dimensions so desktop layouts do not shift.

- [ ] **Step 6: Run tests**

Run:

```bash
npm run test:unit -- tests/unit/permissions.spec.ts tests/unit/auth.store.spec.ts
npm run build
```

Expected: permission and auth tests pass; build succeeds.

- [ ] **Step 7: Commit**

```bash
git add src/app src/stores src/layouts tests/unit/permissions.spec.ts tests/unit/auth.store.spec.ts
git commit -m "feat: add routing permissions and layout shells"
```

### Task 6: Build Shared Components And Public Home

**Files:**
- Create: `src/components/EmptyState.vue`
- Create: `src/components/ForbiddenState.vue`
- Create: `src/components/StatCard.vue`
- Create: `src/modules/post/PostCard.vue`
- Create: `src/modules/post/PostList.vue`
- Create: `src/modules/user/UserSummaryCard.vue`
- Create: `src/pages/public/HomePage.vue`

- [ ] **Step 1: Create shared display components**

Build:

- `EmptyState.vue` with `title`, `description`, and optional action slot.
- `ForbiddenState.vue` with title "无权访问" and a button back to home.
- `StatCard.vue` with `label`, `value`, and `delta` props.

- [ ] **Step 2: Create post cards**

`PostCard.vue` should accept a `post: Post` prop and show title, summary, author, tags, publish time, views, likes, favorites, and comments.

`PostList.vue` should accept `posts: Post[]` and render `PostCard` or `EmptyState`.

- [ ] **Step 3: Build home page**

`HomePage.vue` must include:

- Left menu, friend links, and filing text.
- Category filter row.
- Headline news block.
- Carousel using Element Plus.
- Recommended post list using `getPosts`.
- Right community recommendation block.
- Event calendar block.
- Pagination.

- [ ] **Step 4: Verify home route**

Run:

```bash
npm run build
npm run dev
```

Open `http://localhost:5173/` and confirm the home page renders with mock posts.

- [ ] **Step 5: Commit**

```bash
git add src/components src/modules/post src/modules/user src/pages/public/HomePage.vue
git commit -m "feat: build public home experience"
```

### Task 7: Build Public Detail, Search, Profile, Notifications, And Messages

**Files:**
- Create: `src/modules/post/AuthorSidebar.vue`
- Create: `src/modules/post/InteractionBar.vue`
- Create: `src/modules/comment/CommentList.vue`
- Create: `src/pages/public/PostDetailPage.vue`
- Create: `src/pages/public/SearchPage.vue`
- Create: `src/pages/public/CategoryPage.vue`
- Create: `src/pages/public/UserProfilePage.vue`
- Create: `src/pages/public/NotificationsPage.vue`
- Create: `src/pages/public/MessagesPage.vue`

- [ ] **Step 1: Build detail page modules**

`AuthorSidebar.vue` shows author stats, follow button, private message entrance, hot articles, and latest comments.

`InteractionBar.vue` shows like, dislike, favorite, comment, share, and report actions. Like, dislike, and favorite call `togglePostAction`.

`CommentList.vue` renders first-level comments and a logged-in comment form.

- [ ] **Step 2: Build post detail page**

`PostDetailPage.vue` fetches post and comments by route `id`, sanitizes rendered Markdown through `src/utils/markdown.ts`, and shows empty or error states.

- [ ] **Step 3: Build result pages**

`SearchPage.vue` reads `keyword` from query params and calls `getPosts`.

`CategoryPage.vue` reads `slug` from route params and calls `getPosts`.

`UserProfilePage.vue` calls `getUserProfile` and shows published posts from the same author.

- [ ] **Step 4: Build notifications and messages**

`NotificationsPage.vue` shows notification list and local read state.

`MessagesPage.vue` shows thread list, read-only message detail, and a mock send action that returns a success message.

- [ ] **Step 5: Run verification**

Run:

```bash
npm run build
```

Manually verify:

- `/post/<known-post-id>` renders a post.
- `/search?keyword=Vue` renders filtered posts.
- `/notifications` redirects to login when logged out.
- `/messages` redirects to login when logged out.

- [ ] **Step 6: Commit**

```bash
git add src/modules/post src/modules/comment src/pages/public src/utils/markdown.ts
git commit -m "feat: add public content detail and user interactions"
```

### Task 8: Build Auth Pages And JWT-Style Login Flow

**Files:**
- Create: `src/pages/auth/LoginPage.vue`
- Create: `src/pages/auth/RegisterPage.vue`
- Modify: `src/stores/auth.store.ts`
- Modify: `src/mocks/handlers.ts`

- [ ] **Step 1: Build login page**

`LoginPage.vue` must include username/password fields, validation, submit loading state, and quick-fill buttons for:

- Normal user: `writer` / `password123`
- Admin: `admin` / `admin123`

On success, save token through `authStore.login` and navigate to `redirect` or `/`.

- [ ] **Step 2: Build register page**

`RegisterPage.vue` must include display name, username, password, validation, duplicate username error display, and redirect to `/creator/overview` on success.

- [ ] **Step 3: Strengthen auth mock handlers**

Auth mock must:

- Reject unknown credentials with message "用户名或密码错误".
- Reject duplicate registration usernames with message "用户名已存在".
- Return `{ token, user }` for successful login and register.
- Return current user when token is present.

- [ ] **Step 4: Run verification**

Run:

```bash
npm run build
```

Manually verify:

- `/creator/overview` redirects to `/login`.
- User login reaches `/creator/overview`.
- Admin login can reach `/admin`.
- Normal user visiting `/admin` lands on `/403`.

- [ ] **Step 5: Commit**

```bash
git add src/pages/auth src/stores/auth.store.ts src/mocks/handlers.ts
git commit -m "feat: add jwt style auth flow"
```

### Task 9: Build Creator Center And Editor

**Files:**
- Create: `src/modules/editor/EditorShell.vue`
- Create: `src/modules/analytics/StatsChart.vue`
- Create: `src/pages/creator/CreatorOverviewPage.vue`
- Create: `src/pages/creator/CreatorEditorPage.vue`
- Create: `src/pages/creator/CreatorPostsPage.vue`
- Create: `src/pages/creator/CreatorCommentsPage.vue`
- Create: `src/pages/creator/CreatorColumnsPage.vue`
- Create: `src/pages/creator/CreatorAnalyticsPage.vue`
- Modify: `src/stores/draft.store.ts`

- [ ] **Step 1: Build editor shell**

`EditorShell.vue` must include:

- Markdown editor using `md-editor-v3`.
- Preview mode.
- Rich-text light mode tab that edits the same text field through a textarea or controlled contenteditable area.
- Category selector.
- Tag input.
- Cover URL input or preset selector.
- Summary input.
- Save draft button.
- Publish button.
- AI topic and AI article placeholder buttons opening Element Plus dialogs.

- [ ] **Step 2: Build creator editor page**

`CreatorEditorPage.vue` connects `EditorShell` to `draftStore`, `createDraft`, and `publishPost`. Save draft should show success and keep the page open. Publish should navigate to the created post detail page.

- [ ] **Step 3: Build creator management pages**

Create:

- `CreatorOverviewPage.vue`: stat cards and recent posts.
- `CreatorPostsPage.vue`: table of the current user's posts with status tags.
- `CreatorCommentsPage.vue`: table of comments on the user's posts.
- `CreatorColumnsPage.vue`: simple column cards and create-column entrance.
- `CreatorAnalyticsPage.vue`: trend chart, pie chart, and article data table.

- [ ] **Step 4: Run verification**

Run:

```bash
npm run build
```

Manually verify:

- Logged-in user can save a draft.
- Logged-in user can publish a post.
- AI buttons show placeholder dialogs.
- Creator analytics shows line and pie charts.

- [ ] **Step 5: Commit**

```bash
git add src/modules/editor src/modules/analytics src/pages/creator src/stores/draft.store.ts
git commit -m "feat: build creator center and editor"
```

### Task 10: Build Admin Console

**Files:**
- Create: `src/modules/admin/AdminTablePage.vue`
- Create: `src/pages/admin/AdminDashboardPage.vue`
- Create: `src/pages/admin/AdminUsersPage.vue`
- Create: `src/pages/admin/AdminPostsPage.vue`
- Create: `src/pages/admin/AdminCategoriesPage.vue`
- Create: `src/pages/admin/AdminModeratorsPage.vue`
- Create: `src/pages/admin/AdminSensitiveWordsPage.vue`
- Create: `src/pages/admin/AdminAnalyticsPage.vue`

- [ ] **Step 1: Build reusable admin table page**

`AdminTablePage.vue` should accept title, description, rows, columns, and optional row actions. It should render an Element Plus table, filter input, empty state, and pagination.

- [ ] **Step 2: Build admin dashboard**

`AdminDashboardPage.vue` shows admin metric cards, trend chart, content type chart, and recent platform activity.

- [ ] **Step 3: Build admin list pages**

Create pages:

- `AdminUsersPage.vue`: users, role, status, muteUntil, operation buttons.
- `AdminPostsPage.vue`: posts, author, status, counts, operation buttons.
- `AdminCategoriesPage.vue`: categories, descriptions, post counts, operation buttons.
- `AdminModeratorsPage.vue`: section moderators display list.
- `AdminSensitiveWordsPage.vue`: sensitive words, level, createdAt, operation buttons.
- `AdminAnalyticsPage.vue`: platform charts and top posts.

Operation buttons show confirmation and success messages with local state changes only.

- [ ] **Step 4: Run verification**

Run:

```bash
npm run build
```

Manually verify:

- Admin can open all `/admin/*` pages.
- Normal user cannot open `/admin/*`.
- Admin tables render non-empty data and empty states work when filters hide all rows.

- [ ] **Step 5: Commit**

```bash
git add src/modules/admin src/pages/admin
git commit -m "feat: build admin console prototype"
```

### Task 11: Add Error Pages, SEO Helpers, Responsive Polish, And E2E Smoke Tests

**Files:**
- Create: `src/pages/errors/ForbiddenPage.vue`
- Create: `src/pages/errors/NotFoundPage.vue`
- Create: `src/app/head.ts`
- Modify: public, creator, and admin pages for page title calls.
- Create: `tests/e2e/smoke.spec.ts`

- [ ] **Step 1: Create head helper**

`src/app/head.ts`:

```ts
export function setPageTitle(title: string) {
  document.title = `${title} - CodeNest`;
}

export function setPageDescription(description: string) {
  let meta = document.querySelector<HTMLMetaElement>('meta[name="description"]');
  if (!meta) {
    meta = document.createElement('meta');
    meta.name = 'description';
    document.head.appendChild(meta);
  }
  meta.content = description;
}
```

- [ ] **Step 2: Create error pages**

`ForbiddenPage.vue` uses `ForbiddenState`.

`NotFoundPage.vue` uses `EmptyState` with a button back to `/`.

- [ ] **Step 3: Apply responsive polish**

Ensure:

- Public home becomes single-column under 768px.
- Detail page sidebars stack below content under 900px.
- Creator and admin sidebars collapse under 900px.
- Buttons and table actions wrap instead of overflowing.

- [ ] **Step 4: Write smoke E2E tests**

`tests/e2e/smoke.spec.ts`:

```ts
import { expect, test } from '@playwright/test';

test('home page renders public content', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('CodeNest')).toBeVisible();
  await expect(page.getByText('推荐博客')).toBeVisible();
});

test('guest is redirected from creator center to login', async ({ page }) => {
  await page.goto('/creator/overview');
  await expect(page).toHaveURL(/\/login/);
});

test('admin can open dashboard', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('button', { name: /管理员/ }).click();
  await page.getByRole('button', { name: /登录/ }).click();
  await page.goto('/admin');
  await expect(page.getByText('管理看板')).toBeVisible();
});
```

- [ ] **Step 5: Run full verification**

Run:

```bash
npm run build
npm run test:unit
npm run test:e2e
```

Expected: all checks pass on desktop and mobile Playwright projects.

- [ ] **Step 6: Commit**

```bash
git add src/pages/errors src/app/head.ts src tests/e2e/smoke.spec.ts
git commit -m "test: add responsive smoke verification"
```

### Task 12: Final Product Review And Documentation Update

**Files:**
- Create or modify: `README.md`
- Review: all files changed in Tasks 1-11 through `git diff` before the final commit.

- [ ] **Step 1: Create README**

`README.md` must include:

```md
# CodeNest

CodeNest is a Vue 3 frontend prototype for a technical blog and forum platform.

## Tech Stack

- Vue 3
- TypeScript
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios
- Mock API

## Development

\`\`\`bash
npm install
npm run dev
\`\`\`

## Verification

\`\`\`bash
npm run build
npm run test:unit
npm run test:e2e
\`\`\`

## Test Accounts

- Normal user: `writer` / `password123`
- Admin: `admin` / `admin123`

## Scope

V1 is frontend-only. Backend integration, real-time messaging, real AI generation, and deep moderation workflows are outside V1.
```

- [ ] **Step 2: Run final checks**

Run:

```bash
npm run build
npm run test:unit
npm run test:e2e
git status --short
```

Expected:

- Build succeeds.
- Unit tests pass.
- E2E tests pass.
- `git status --short` only shows intended README or final polish changes before commit.

- [ ] **Step 3: Commit final documentation**

```bash
git add README.md src tests package.json package-lock.json vite.config.ts vitest.config.ts playwright.config.ts netlify.toml
git commit -m "docs: document frontend prototype workflow"
```

## Verification Checklist

Before calling the implementation complete, verify:

- Guest can browse home, search, category, post detail, and user profile pages.
- Guest is redirected from creator and admin routes.
- Normal user can log in, use creator center, publish a mock post, comment, like, favorite, follow, see notifications, and see messages.
- Normal user cannot access admin pages.
- Admin can log in and access dashboard, user list, blog list, category list, moderator list, sensitive word list, and analytics.
- Markdown content renders through the sanitize utility.
- Empty states appear for filtered empty tables and lists.
- Desktop viewport is polished.
- Mobile viewport remains readable and does not overlap.
- Netlify SPA redirect is configured.
