<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth.store';

interface RegisterForm {
  displayName: string;
  username: string;
  password: string;
}

const router = useRouter();
const authStore = useAuthStore();

const formRef = ref<FormInstance>();
const isSubmitting = ref(false);
const serviceError = ref('');
const form = reactive<RegisterForm>({
  displayName: '',
  username: '',
  password: '',
});

const rules = reactive<FormRules<RegisterForm>>({
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { min: 2, message: '显示名称至少 2 位', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少 3 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
});

async function submitRegister() {
  if (!formRef.value) return;

  serviceError.value = '';
  await formRef.value.validate();

  isSubmitting.value = true;
  try {
    await authStore.register({ ...form });
    await router.push('/creator/overview');
  } catch (error) {
    serviceError.value = error instanceof Error ? error.message : '注册失败，请稍后重试';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="register-title">
      <div class="auth-panel__header">
        <p class="auth-panel__eyebrow">CodeNest</p>
        <h1 id="register-title">注册</h1>
        <p>创建账号后直接进入创作中心。</p>
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
        @submit.prevent="submitRegister"
      >
        <el-form-item label="显示名称" prop="displayName">
          <el-input
            v-model.trim="form.displayName"
            name="displayName"
            autocomplete="name"
            placeholder="请输入显示名称"
            :disabled="isSubmitting"
          />
        </el-form-item>

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
            autocomplete="new-password"
            placeholder="请输入密码"
            show-password
            :disabled="isSubmitting"
          />
        </el-form-item>

        <el-button
          class="auth-form__submit"
          type="primary"
          native-type="submit"
          :loading="isSubmitting"
        >
          注册并进入创作中心
        </el-button>
      </el-form>

      <p class="auth-panel__switch">
        已有账号？
        <RouterLink to="/login">返回登录</RouterLink>
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
    linear-gradient(135deg, rgb(247 250 252 / 90%), rgb(238 247 244 / 90%)),
    url('https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1600&q=80')
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
  color: #047857;
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

.auth-form__submit {
  width: 100%;
  margin-top: 4px;
}

.auth-panel__switch {
  margin: 22px 0 0;
  color: #4b5563;
  font-size: 14px;
  text-align: center;
}

.auth-panel__switch a {
  color: #047857;
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
