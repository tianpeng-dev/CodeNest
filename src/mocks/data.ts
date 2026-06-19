import type { SensitiveWord } from '../types/admin';
import type { PiePoint, TrendPoint } from '../types/analytics';
import type { Comment } from '../types/comment';
import type { MessageItem, MessageThread } from '../types/message';
import type { NotificationItem } from '../types/notification';
import type { Category, Post } from '../types/post';
import type { User } from '../types/user';

const adminUser: User = {
  id: 'user-001',
  username: 'lin-admin',
  displayName: '林栖',
  avatarUrl: 'https://api.dicebear.com/9.x/initials/svg?seed=Lin',
  bio: 'CodeNest 社区管理员，关注前端工程化与内容治理。',
  role: 'admin',
  status: 'active',
  muteUntil: null,
  postCount: 18,
  likeCount: 642,
  favoriteCount: 136,
  followerCount: 1280,
};

const memberUser: User = {
  id: 'user-002',
  username: 'chen-dev',
  displayName: '陈一鸣',
  avatarUrl: 'https://api.dicebear.com/9.x/initials/svg?seed=Chen',
  bio: '全栈开发者，喜欢写 Vue、Node.js 和可维护的文档。',
  role: 'user',
  status: 'banned',
  muteUntil: '2026-07-01T09:00:00.000Z',
  postCount: 7,
  likeCount: 215,
  favoriteCount: 48,
  followerCount: 320,
};

const productUser: User = {
  id: 'user-003',
  username: 'tang-pm',
  displayName: '唐青',
  avatarUrl: 'https://api.dicebear.com/9.x/initials/svg?seed=Tang',
  bio: '技术产品经理，长期记录社区增长、数据分析和协作流程。',
  role: 'user',
  status: 'active',
  muteUntil: null,
  postCount: 12,
  likeCount: 388,
  favoriteCount: 91,
  followerCount: 540,
};

export const mockUsers: User[] = [adminUser, memberUser, productUser];

const frontendCategory: Category = {
  id: 'cat-frontend',
  name: '前端工程',
  slug: 'frontend',
  description: 'Vue、React、构建工具与浏览器端体验优化。',
  postCount: 28,
};

const backendCategory: Category = {
  id: 'cat-backend',
  name: '后端架构',
  slug: 'backend',
  description: 'API 设计、数据库、缓存和服务端稳定性实践。',
  postCount: 21,
};

const growthCategory: Category = {
  id: 'cat-growth',
  name: '社区运营',
  slug: 'community-growth',
  description: '内容增长、用户激励、审核策略和产品分析。',
  postCount: 14,
};

export const mockCategories: Category[] = [
  frontendCategory,
  backendCategory,
  growthCategory,
];

export const mockPosts: Post[] = [
  {
    id: 'post-001',
    title: 'Vue 组合式函数的边界怎么划',
    summary: '从 CodeNest 编辑器页的抽象出发，讨论状态、请求与副作用的拆分方式。',
    content: '组合式函数不只是复用代码，更重要的是给复杂页面留出清晰的维护边界。',
    coverUrl: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085',
    author: adminUser,
    category: frontendCategory,
    tags: ['Vue', 'Composition API', '可维护性'],
    status: 'published',
    viewCount: 3280,
    likeCount: 246,
    favoriteCount: 112,
    commentCount: 18,
    createdAt: '2026-05-02T08:30:00.000Z',
    updatedAt: '2026-05-05T10:15:00.000Z',
    publishedAt: '2026-05-02T09:00:00.000Z',
  },
  {
    id: 'post-002',
    title: '用 Pinia 管理论坛草稿箱',
    summary: '记录帖子草稿、自动保存和离线恢复的状态建模思路。',
    content: '草稿箱的核心是让用户安心编辑，状态结构应围绕恢复和冲突处理设计。',
    coverUrl: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4',
    author: memberUser,
    category: frontendCategory,
    tags: ['Pinia', '草稿箱', '状态管理'],
    status: 'draft',
    viewCount: 120,
    likeCount: 9,
    favoriteCount: 5,
    commentCount: 0,
    createdAt: '2026-05-08T14:20:00.000Z',
    updatedAt: '2026-05-10T07:45:00.000Z',
    publishedAt: null,
  },
  {
    id: 'post-003',
    title: 'NestJS 中间件与守卫的职责拆分',
    summary: '通过登录鉴权和管理后台权限控制，解释中间件、守卫、拦截器的分工。',
    content: '权限逻辑越靠近业务入口，越需要把认证、授权和审计日志分层处理。',
    coverUrl: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31',
    author: adminUser,
    category: backendCategory,
    tags: ['NestJS', 'Auth', 'RBAC'],
    status: 'published',
    viewCount: 2140,
    likeCount: 180,
    favoriteCount: 76,
    commentCount: 11,
    createdAt: '2026-05-12T02:10:00.000Z',
    updatedAt: '2026-05-12T04:35:00.000Z',
    publishedAt: '2026-05-12T04:35:00.000Z',
  },
  {
    id: 'post-004',
    title: '审核队列里的高风险关键词策略',
    summary: '梳理敏感词等级、人工复核和误伤申诉的产品机制。',
    content: '内容治理需要兼顾效率和透明度，高风险命中应进入可追踪的复核链路。',
    coverUrl: 'https://images.unsplash.com/photo-1450101499163-c8848c66ca85',
    author: productUser,
    category: growthCategory,
    tags: ['审核', '敏感词', '运营'],
    status: 'hidden',
    viewCount: 860,
    likeCount: 41,
    favoriteCount: 19,
    commentCount: 3,
    createdAt: '2026-05-15T11:00:00.000Z',
    updatedAt: '2026-05-17T03:25:00.000Z',
    publishedAt: '2026-05-15T12:00:00.000Z',
  },
  {
    id: 'post-005',
    title: 'Redis 缓存穿透的三个兜底方案',
    summary: '从空值缓存、布隆过滤器和限流策略分析社区帖子详情页的缓存保护。',
    content: '高频读取接口不能只依赖缓存命中，还要考虑异常请求对数据库的压力。',
    coverUrl: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71',
    author: memberUser,
    category: backendCategory,
    tags: ['Redis', '缓存', '性能'],
    status: 'deleted',
    viewCount: 540,
    likeCount: 22,
    favoriteCount: 8,
    commentCount: 1,
    createdAt: '2026-05-18T06:40:00.000Z',
    updatedAt: '2026-05-19T01:20:00.000Z',
    publishedAt: '2026-05-18T07:00:00.000Z',
  },
  {
    id: 'post-006',
    title: '社区冷启动时最该看的五个指标',
    summary: '用发帖率、评论率、收藏率、留存和举报率判断技术社区是否健康。',
    content: '冷启动阶段的目标不是追求总量，而是找到能持续产生讨论的内容结构。',
    coverUrl: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71',
    author: productUser,
    category: growthCategory,
    tags: ['数据分析', '增长', '社区'],
    status: 'published',
    viewCount: 1760,
    likeCount: 138,
    favoriteCount: 64,
    commentCount: 9,
    createdAt: '2026-05-22T09:45:00.000Z',
    updatedAt: '2026-05-23T03:10:00.000Z',
    publishedAt: '2026-05-22T10:00:00.000Z',
  },
];

export const mockComments: Comment[] = [
  {
    id: 'comment-001',
    postId: 'post-001',
    author: memberUser,
    content: '把请求状态单独抽出去这点很实用，编辑器页面确实会变清爽。',
    createdAt: '2026-05-02T10:12:00.000Z',
  },
  {
    id: 'comment-002',
    postId: 'post-003',
    author: productUser,
    content: '权限和审计日志分层后，后台操作记录也会更容易解释。',
    createdAt: '2026-05-12T05:02:00.000Z',
  },
  {
    id: 'comment-003',
    postId: 'post-006',
    author: adminUser,
    content: '建议后续补一篇指标口径说明，方便团队统一看板。',
    createdAt: '2026-05-23T04:18:00.000Z',
  },
];

export const mockNotifications: NotificationItem[] = [
  {
    id: 'notice-001',
    title: '帖子收到新评论',
    content: '陈一鸣评论了你的文章《Vue 组合式函数的边界怎么划》。',
    readAt: null,
    createdAt: '2026-05-02T10:12:30.000Z',
  },
  {
    id: 'notice-002',
    title: '内容审核提醒',
    content: '你的帖子《审核队列里的高风险关键词策略》已进入人工复核。',
    readAt: '2026-05-17T04:00:00.000Z',
    createdAt: '2026-05-17T03:25:30.000Z',
  },
  {
    id: 'notice-003',
    title: '收藏数增长',
    content: '《社区冷启动时最该看的五个指标》今天新增 12 次收藏。',
    readAt: null,
    createdAt: '2026-05-23T12:00:00.000Z',
  },
];

export const mockThreads: MessageThread[] = [
  {
    id: 'thread-001',
    participant: memberUser,
    lastMessage: '我会先补充草稿恢复的边界条件。',
    unreadCount: 1,
    updatedAt: '2026-05-24T09:15:00.000Z',
  },
  {
    id: 'thread-002',
    participant: productUser,
    lastMessage: '本周看板我已经按新口径调整好了。',
    unreadCount: 0,
    updatedAt: '2026-05-24T02:30:00.000Z',
  },
];

export const mockMessages: MessageItem[] = [
  {
    id: 'message-001',
    threadId: 'thread-001',
    sender: adminUser,
    content: '草稿箱那篇文章可以补一段冲突恢复吗？',
    readAt: '2026-05-24T09:05:00.000Z',
    createdAt: '2026-05-24T09:00:00.000Z',
  },
  {
    id: 'message-002',
    threadId: 'thread-001',
    sender: memberUser,
    content: '我会先补充草稿恢复的边界条件。',
    readAt: null,
    createdAt: '2026-05-24T09:15:00.000Z',
  },
  {
    id: 'message-003',
    threadId: 'thread-002',
    sender: productUser,
    content: '本周看板我已经按新口径调整好了。',
    readAt: '2026-05-24T02:45:00.000Z',
    createdAt: '2026-05-24T02:30:00.000Z',
  },
];

export const mockTrend: TrendPoint[] = [
  { date: '2026-05-18', value: 132 },
  { date: '2026-05-19', value: 148 },
  { date: '2026-05-20', value: 171 },
  { date: '2026-05-21', value: 166 },
  { date: '2026-05-22', value: 203 },
  { date: '2026-05-23', value: 226 },
  { date: '2026-05-24', value: 218 },
];

export const mockPie: PiePoint[] = [
  { name: '前端工程', value: 42 },
  { name: '后端架构', value: 34 },
  { name: '社区运营', value: 24 },
];

export const mockSensitiveWords: SensitiveWord[] = [
  {
    id: 'word-001',
    word: '外挂',
    level: 'medium',
    createdAt: '2026-04-10T08:00:00.000Z',
    hitCount: 18,
  },
  {
    id: 'word-002',
    word: '刷量',
    level: 'low',
    createdAt: '2026-04-12T08:00:00.000Z',
    hitCount: 9,
  },
  {
    id: 'word-003',
    word: '数据泄露',
    level: 'high',
    createdAt: '2026-04-15T08:00:00.000Z',
    hitCount: 4,
  },
];
