import { defineComponent, h, nextTick } from 'vue';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia, type Pinia } from 'pinia';
import LoginPage from '@/pages/auth/LoginPage.vue';
import RegisterPage from '@/pages/auth/RegisterPage.vue';
import { useAuthStore } from '@/stores/auth.store';

const { loginMock, registerMock, pushMock, routeQuery } = vi.hoisted(() => ({
  loginMock: vi.fn(),
  registerMock: vi.fn(),
  pushMock: vi.fn(),
  routeQuery: {
    value: {} as Record<string, unknown>,
  },
}));

vi.mock('@/services/auth.service', () => ({
  login: loginMock,
  register: registerMock,
  logout: vi.fn(),
  getCurrentUser: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: routeQuery.value }),
  useRouter: () => ({ push: pushMock }),
}));

const authSession = {
  token: 'mock-token',
  expiresAt: '2026-06-19T00:00:00.000Z',
  user: {
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
  },
};

const formStub = defineComponent({
  name: 'ElForm',
  setup(_, { attrs, slots, expose }) {
    expose({
      validate: vi.fn(() => Promise.resolve(true)),
    });

    return () => h('form', attrs, slots.default?.());
  },
});

const formItemStub = defineComponent({
  name: 'ElFormItem',
  setup(_, { slots }) {
    return () => h('label', slots.default?.());
  },
});

const inputStub = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    name: { type: String, default: '' },
    type: { type: String, default: 'text' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    return () =>
      h('input', {
        ...attrs,
        name: props.name,
        type: props.type,
        value: props.modelValue,
        disabled: props.disabled,
        onInput: (event: Event) => {
          emit('update:modelValue', (event.target as HTMLInputElement).value);
        },
      });
  },
});

const buttonStub = defineComponent({
  name: 'ElButton',
  props: {
    loading: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    nativeType: { type: String, default: 'button' },
  },
  setup(props, { attrs, slots }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: props.nativeType,
          disabled: props.disabled || props.loading,
          'data-loading': String(props.loading),
        },
        slots.default?.(),
      );
  },
});

const alertStub = defineComponent({
  name: 'ElAlert',
  props: {
    title: { type: String, default: '' },
  },
  setup(props) {
    return () => h('div', { role: 'alert' }, props.title);
  },
});

const routerLinkStub = defineComponent({
  name: 'RouterLink',
  props: {
    to: { type: [String, Object], required: true },
  },
  setup(props, { slots }) {
    return () => h('a', { href: typeof props.to === 'string' ? props.to : '#' }, slots.default?.());
  },
});

let pinia: Pinia;

function mountAuthPage(component: typeof LoginPage | typeof RegisterPage) {
  return mount(component, {
    global: {
      plugins: [pinia],
      stubs: {
        ElForm: formStub,
        ElFormItem: formItemStub,
        ElInput: inputStub,
        ElButton: buttonStub,
        ElAlert: alertStub,
        RouterLink: routerLinkStub,
      },
    },
  });
}

function inputValue(wrapper: ReturnType<typeof mount>, name: string) {
  return (wrapper.find(`input[name="${name}"]`).element as HTMLInputElement).value;
}

async function fillInput(wrapper: ReturnType<typeof mount>, name: string, value: string) {
  await wrapper.find(`input[name="${name}"]`).setValue(value);
}

async function submit(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('form').trigger('submit');
  await flushPromises();
  await nextTick();
}

describe('login page', () => {
  beforeEach(() => {
    loginMock.mockReset();
    registerMock.mockReset();
    pushMock.mockReset();
    routeQuery.value = {};
    window.localStorage.clear();
    pinia = createPinia();
    setActivePinia(pinia);
  });

  it('quick-fills the writer account credentials', async () => {
    const wrapper = mountAuthPage(LoginPage);
    const writerButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('writer / password123'));

    await writerButton?.trigger('click');

    expect(inputValue(wrapper, 'username')).toBe('writer');
    expect(inputValue(wrapper, 'password')).toBe('password123');
  });

  it('shows the login service error and clears loading after a failed submit', async () => {
    loginMock.mockRejectedValueOnce(new Error('用户名或密码错误'));
    const wrapper = mountAuthPage(LoginPage);
    const authStore = useAuthStore();

    await fillInput(wrapper, 'username', 'writer');
    await fillInput(wrapper, 'password', 'wrong-password');
    await submit(wrapper);

    expect(wrapper.text()).toContain('用户名或密码错误');
    expect(pushMock).not.toHaveBeenCalled();
    expect(authStore.isLoggedIn).toBe(false);
    expect(wrapper.find('button[type="submit"]').attributes('data-loading')).toBe('false');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined();
  });

  it('logs in and redirects to the redirect query', async () => {
    routeQuery.value = { redirect: '/creator/overview' };
    loginMock.mockResolvedValueOnce(authSession);
    const wrapper = mountAuthPage(LoginPage);
    const authStore = useAuthStore();

    await fillInput(wrapper, 'username', 'writer');
    await fillInput(wrapper, 'password', 'password123');
    await submit(wrapper);

    expect(loginMock).toHaveBeenCalledWith({
      username: 'writer',
      password: 'password123',
    });
    expect(authStore.token).toBe('mock-token');
    expect(pushMock).toHaveBeenCalledWith('/creator/overview');
  });

  it('logs in and redirects home when no redirect query is provided', async () => {
    loginMock.mockResolvedValueOnce(authSession);
    const wrapper = mountAuthPage(LoginPage);

    await fillInput(wrapper, 'username', 'writer');
    await fillInput(wrapper, 'password', 'password123');
    await submit(wrapper);

    expect(pushMock).toHaveBeenCalledWith('/');
  });
});

describe('register page', () => {
  beforeEach(() => {
    loginMock.mockReset();
    registerMock.mockReset();
    pushMock.mockReset();
    routeQuery.value = {};
    window.localStorage.clear();
    pinia = createPinia();
    setActivePinia(pinia);
  });

  it('shows the duplicate username service error', async () => {
    registerMock.mockRejectedValueOnce(new Error('用户名已存在'));
    const wrapper = mountAuthPage(RegisterPage);

    await fillInput(wrapper, 'displayName', '新作者');
    await fillInput(wrapper, 'username', 'writer');
    await fillInput(wrapper, 'password', 'password123');
    await submit(wrapper);

    expect(wrapper.text()).toContain('用户名已存在');
    expect(pushMock).not.toHaveBeenCalled();
    expect(wrapper.find('button[type="submit"]').attributes('data-loading')).toBe('false');
  });

  it('registers and redirects to creator overview', async () => {
    registerMock.mockResolvedValueOnce(authSession);
    const wrapper = mountAuthPage(RegisterPage);
    const authStore = useAuthStore();

    await fillInput(wrapper, 'displayName', '新作者');
    await fillInput(wrapper, 'username', 'new-writer');
    await fillInput(wrapper, 'password', 'password123');
    await submit(wrapper);

    expect(registerMock).toHaveBeenCalledWith({
      displayName: '新作者',
      username: 'new-writer',
      password: 'password123',
    });
    expect(authStore.token).toBe('mock-token');
    expect(pushMock).toHaveBeenCalledWith('/creator/overview');
  });
});
