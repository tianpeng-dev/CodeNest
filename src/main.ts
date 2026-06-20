import { clerkPlugin } from '@clerk/vue';
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

const PUBLISHABLE_KEY = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;
const useMockApi = import.meta.env.VITE_USE_MOCK_API === 'true';

if (!PUBLISHABLE_KEY) {
  throw new Error('Missing VITE_CLERK_PUBLISHABLE_KEY. Add your key to .env.local, then restart the dev server.');
}

if (useMockApi) {
  setupMockApi();
}

createApp(App)
  .use(createPinia())
  .use(clerkPlugin, {
    publishableKey: PUBLISHABLE_KEY,
    signInUrl: '/sign-in',
    signUpUrl: '/sign-up',
  })
  .use(router)
  .use(ElementPlus)
  .mount('#app');
