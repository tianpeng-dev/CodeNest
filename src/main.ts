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
