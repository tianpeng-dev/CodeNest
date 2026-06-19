<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth.store';

interface LoginForm {
  username: string;
  password: string;
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const formRef = ref<FormInstance>();
const isSubmitting = ref(false);
const serviceError = ref('');
const form = reactive<LoginForm>({
  username: '',
  password: '',
});

const rules = reactive<FormRules<LoginForm>>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
});

const quickAccounts = [
  { label: '普通用户 writer / password123', username: 'writer', password: 'password123' },
  { label: '管理员 admin / admin123', username: 'admin', password: 'admin123' },
];

const redirectPath = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect ? redirect : '/';
});

function fillAccount(account: LoginForm) {
  form.username = account.username;
  form.password = account.password;
  serviceError.value = '';
}

async function submitLogin() {
  if (!formRef.value) return;

  serviceError.value = '';
  await formRef.value.validate();

  isSubmitting.value = true;
  try {
    await authStore.login({ ...form });
    await router.push(redirectPath.value);
  } catch (error) {
    serviceError.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="login-title">
      <div class="auth-panel__header">
        <p class="auth-panel__eyebrow">CodeNest</p>
        <h1 id="login-title">登录</h1>
        <p>进入创作中心、通知和私信。</p>
      </div>

      <el-alert
        v-if="serviceError"
        class="auth-panel__error"
        :title="serviceError"
        type="error"
        show-icon
        :closable="false"
      />

      <el-form
        ref="formRef"
        class="auth-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submitLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="form.username"
            name="username"
            autocomplete="username"
            placeholder="请输入用户名"
            :disabled="isSubmitting"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            name="password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            :disabled="isSubmitting"
          />
        </el-form-item>

        <div class="auth-form__quick-fill" aria-label="快速填充登录账号">
          <el-button
            v-for="account in quickAccounts"
            :key="account.username"
            type="primary"
            plain
            :disabled="isSubmitting"
            @click="fillAccount(account)"
          >
            {{ account.label }}
          </el-button>
        </div>

        <el-button
          class="auth-form__submit"
          type="primary"
          native-type="submit"
          :loading="isSubmitting"
        >
          登录
        </el-button>
      </el-form>

      <p class="auth-panel__switch">
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 64px);
  display: grid;
  place-items: center;
  padding: 48px 20px;
  background:
    linear-gradient(135deg, rgb(245 247 250 / 88%), rgb(236 242 255 / 88%)),
    url('https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1600&q=80')
      center / cover;
}

.auth-panel {
  width: min(100%, 440px);
  padding: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 18px 48px rgb(15 23 42 / 12%);
}

.auth-panel__header {
  margin-bottom: 24px;
}

.auth-panel__eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.auth-panel h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  line-height: 1.2;
}

.auth-panel__header p:not(.auth-panel__eyebrow) {
  margin: 10px 0 0;
  color: #4b5563;
  font-size: 14px;
}

.auth-panel__error {
  margin-bottom: 18px;
}

.auth-form__quick-fill {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.auth-form__quick-fill :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.auth-form__submit {
  width: 100%;
}

.auth-panel__switch {
  margin: 22px 0 0;
  color: #4b5563;
  font-size: 14px;
  text-align: center;
}

.auth-panel__switch a {
  color: #2563eb;
  font-weight: 700;
  text-decoration: none;
}

.auth-panel__switch a:hover {
  text-decoration: underline;
}

@media (max-width: 520px) {
  .auth-page {
    padding: 28px 16px;
  }

  .auth-panel {
    padding: 24px;
  }
}
</style>
