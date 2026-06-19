import { createRouter, createWebHistory } from 'vue-router';
import { canAccessRoute, type RouteAccess } from './permissions';
import { setPageDescription, setPageTitle } from './head';
import { useAuthStore } from '@/stores/auth.store';

declare module 'vue-router' {
  interface RouteMeta {
    access?: RouteAccess;
    description?: string;
    title?: string;
  }
}

function loginRedirect(target: string) {
  return {
    path: '/login',
    query: {
      redirect: target,
    },
  };
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/pages/public/HomePage.vue'),
          meta: {
            access: 'public',
            title: '首页',
            description: 'CodeNest 技术社区首页，发现推荐博客、热门讨论和创作者内容。',
          },
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/pages/public/SearchPage.vue'),
          meta: {
            access: 'public',
            title: '搜索',
            description: '搜索 CodeNest 技术文章、分类和社区讨论。',
          },
        },
        {
          path: 'category/:slug',
          name: 'category',
          component: () => import('@/pages/public/CategoryPage.vue'),
          meta: {
            access: 'public',
            title: '分类',
            description: '浏览 CodeNest 分类频道下的精选技术内容。',
          },
        },
        {
          path: 'post/:id',
          name: 'post-detail',
          component: () => import('@/pages/public/PostDetailPage.vue'),
          meta: {
            access: 'public',
            title: '帖子详情',
            description: '阅读 CodeNest 技术文章详情、评论和作者动态。',
          },
        },
        {
          path: 'u/:id',
          name: 'user-profile',
          component: () => import('@/pages/public/UserProfilePage.vue'),
          meta: { access: 'public', title: '用户主页' },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/pages/public/NotificationsPage.vue'),
          meta: { access: 'user', title: '通知' },
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/pages/public/MessagesPage.vue'),
          meta: { access: 'user', title: '私信' },
        },
      ],
    },
    {
      path: '/creator',
      component: () => import('@/layouts/UserCenterLayout.vue'),
      meta: { access: 'user' },
      children: [
        {
          path: 'overview',
          name: 'creator-overview',
          component: () => import('@/pages/creator/CreatorOverviewPage.vue'),
          meta: { access: 'user', title: '创作中心' },
        },
        {
          path: 'editor',
          name: 'creator-editor',
          component: () => import('@/pages/creator/CreatorEditorPage.vue'),
          meta: { access: 'user', title: '编辑器' },
        },
        {
          path: 'posts',
          name: 'creator-posts',
          component: () => import('@/pages/creator/CreatorPostsPage.vue'),
          meta: { access: 'user', title: '内容管理' },
        },
        {
          path: 'comments',
          name: 'creator-comments',
          component: () => import('@/pages/creator/CreatorCommentsPage.vue'),
          meta: { access: 'user', title: '评论管理' },
        },
        {
          path: 'columns',
          name: 'creator-columns',
          component: () => import('@/pages/creator/CreatorColumnsPage.vue'),
          meta: { access: 'user', title: '专栏管理' },
        },
        {
          path: 'analytics',
          name: 'creator-analytics',
          component: () => import('@/pages/creator/CreatorAnalyticsPage.vue'),
          meta: { access: 'user', title: '创作数据' },
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { access: 'admin' },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/pages/admin/AdminDashboardPage.vue'),
          meta: { access: 'admin', title: '管理后台' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/pages/admin/AdminUsersPage.vue'),
          meta: { access: 'admin', title: '用户管理' },
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: () => import('@/pages/admin/AdminPostsPage.vue'),
          meta: { access: 'admin', title: '帖子管理' },
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/pages/admin/AdminCategoriesPage.vue'),
          meta: { access: 'admin', title: '分类管理' },
        },
        {
          path: 'moderators',
          name: 'admin-moderators',
          component: () => import('@/pages/admin/AdminModeratorsPage.vue'),
          meta: { access: 'admin', title: '版主管理' },
        },
        {
          path: 'sensitive-words',
          name: 'admin-sensitive-words',
          component: () => import('@/pages/admin/AdminSensitiveWordsPage.vue'),
          meta: { access: 'admin', title: '敏感词' },
        },
        {
          path: 'analytics',
          name: 'admin-analytics',
          component: () => import('@/pages/admin/AdminAnalyticsPage.vue'),
          meta: { access: 'admin', title: '运营分析' },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/auth/LoginPage.vue'),
      meta: {
        access: 'public',
        title: '登录',
        description: '登录 CodeNest，进入创作中心、通知和私信。',
      },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/auth/RegisterPage.vue'),
      meta: { access: 'public', title: '注册' },
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('@/pages/errors/ForbiddenPage.vue'),
      meta: { access: 'public', title: '无权访问' },
    },
    {
      path: '/404',
      name: 'not-found',
      component: () => import('@/pages/errors/NotFoundPage.vue'),
      meta: {
        access: 'public',
        title: '页面不存在',
        description: '当前 CodeNest 页面不存在或已经移动。',
      },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404',
    },
  ],
});

router.beforeEach(async (to) => {
  const access = to.meta.access;

  if (!access || access === 'public') {
    return true;
  }

  const authStore = useAuthStore();

  if (authStore.token && !authStore.currentUser) {
    try {
      await authStore.loadCurrentUser();
    } catch {
      return loginRedirect(to.fullPath);
    }
  }

  if (!authStore.token || !authStore.currentUser) {
    return loginRedirect(to.fullPath);
  }

  if (!canAccessRoute(access, authStore.currentUser)) {
    return { path: '/403' };
  }

  return true;
});

router.afterEach((to) => {
  setPageTitle(to.meta.title);
  setPageDescription(
    to.meta.description ?? 'CodeNest 技术社区，连接创作者、读者与高质量技术讨论。',
  );
});
