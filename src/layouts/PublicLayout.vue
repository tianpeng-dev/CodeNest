<script setup lang="ts">
import { Show, SignInButton, SignUpButton, UserButton } from '@clerk/vue';
import { ref, watch } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { Menu, Plus, Search } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const searchKeyword = ref(String(route.query.keyword ?? ''));

function submitHeaderSearch() {
  const keyword = searchKeyword.value.trim();

  router.push({
    path: '/search',
    query: keyword ? { keyword } : undefined,
  });
}

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = String(keyword ?? '');
  },
);
</script>

<template>
  <el-container class="public-layout">
    <el-header class="public-layout__header">
      <div class="public-layout__bar page-shell">
        <div class="public-layout__brand-group">
          <button class="public-layout__menu-trigger" type="button" aria-label="主菜单">
            <el-icon><Menu /></el-icon>
          </button>
          <RouterLink class="public-layout__brand" to="/">CodeNest</RouterLink>
        </div>

        <form class="public-layout__search" role="search" @submit.prevent="submitHeaderSearch">
          <el-input
            v-model="searchKeyword"
            class="public-layout__search-input"
            placeholder="搜索技术文章、问题和作者"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <button class="public-layout__search-button" type="submit">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </button>
        </form>

        <div class="public-layout__actions" aria-label="账号与创作入口">
          <Show when="signed-out">
            <SignInButton mode="redirect" fallback-redirect-url="/">
              <button class="public-layout__login" type="button">登录</button>
            </SignInButton>
            <SignUpButton mode="redirect" fallback-redirect-url="/creator/overview">
              <button class="public-layout__register" type="button">注册</button>
            </SignUpButton>
          </Show>
          <Show when="signed-in">
            <UserButton after-sign-out-url="/" />
          </Show>
          <RouterLink
            class="public-layout__post-button"
            to="/creator/editor"
            aria-label="发帖"
            title="发帖"
          >
            <el-icon><Plus /></el-icon>
            <span>发帖</span>
          </RouterLink>
        </div>
      </div>
    </el-header>
    <el-main class="public-layout__main page-shell">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.public-layout__header {
  height: 64px;
  padding: 0;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.public-layout__bar {
  display: grid;
  grid-template-columns: 220px minmax(320px, 640px) 320px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  height: 64px;
}

.public-layout__brand-group {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.public-layout__menu-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: #172033;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.public-layout__menu-trigger:hover {
  background: #f3f4f6;
}

.public-layout__brand {
  flex: 0 0 auto;
  color: #172033;
  font-size: 21px;
  font-weight: 800;
}

.public-layout__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.public-layout__search-input :deep(.el-input__wrapper) {
  height: 34px;
  border-radius: 999px;
  box-shadow: 0 0 0 1px #ff5a3d inset;
}

.public-layout__search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ff4d2f inset;
}

.public-layout__search-button,
.public-layout__post-button,
.public-layout__login,
.public-layout__register {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  white-space: nowrap;
  text-decoration: none;
  border: 0;
  cursor: pointer;
}

.public-layout__search-button {
  gap: 6px;
  height: 34px;
  padding: 0 18px;
  color: #ffffff;
  font: inherit;
  font-weight: 700;
  background: #ff5a3d;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.public-layout__search-button:hover {
  background: #f0442c;
}

.public-layout__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  min-width: 0;
}

.public-layout__login {
  width: 40px;
  height: 40px;
  color: #344054;
  font: inherit;
  font-size: 14px;
  background: #f0f1f5;
  border-radius: 999px;
}

.public-layout__login:hover {
  color: #172033;
  background: #e7e9ef;
}

.public-layout__register {
  color: #ff5a3d;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  background: transparent;
}

.public-layout__register:hover {
  color: #f0442c;
}

.public-layout__post-button {
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  background: #ff5a3d;
  border-radius: 999px;
}

.public-layout__post-button:hover {
  background: #f0442c;
}

.public-layout__main {
  min-height: calc(100vh - 64px);
  padding: 28px 0 40px;
}

@media (max-width: 1100px) {
  .public-layout__bar {
    grid-template-columns: auto minmax(260px, 1fr) auto;
    gap: 14px;
  }
}

@media (max-width: 760px) {
  .public-layout__header {
    height: auto;
  }

  .public-layout__bar {
    grid-template-columns: 1fr auto;
    height: auto;
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .public-layout__brand {
    font-size: 18px;
  }

  .public-layout__search {
    grid-column: 1 / -1;
    grid-row: 2;
    order: 3;
  }

  .public-layout__actions {
    gap: 10px;
  }

  .public-layout__login {
    width: auto;
    height: 34px;
    padding: 0 12px;
  }

  .public-layout__post-button {
    height: 34px;
    padding: 0 14px;
  }
}

@media (max-width: 480px) {
  .public-layout__search {
    grid-template-columns: minmax(0, 1fr) 72px;
    gap: 8px;
  }

  .public-layout__search-button {
    padding: 0 12px;
  }

  .public-layout__post-button span {
    display: none;
  }

  .public-layout__post-button {
    width: 38px;
    padding: 0;
  }
}
</style>
