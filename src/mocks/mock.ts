import AxiosMockAdapter from 'axios-mock-adapter';
import { http } from '../services/http';
import { registerMockHandlers, resetMockState } from './handlers';

let installed = false;

export function setupMockApi() {
  if (installed) {
    return;
  }

  const mock = new AxiosMockAdapter(http, { delayResponse: 300 });
  registerMockHandlers(mock);
  installed = true;
}

export function resetMockApi() {
  resetMockState();

  if (!installed) {
    setupMockApi();
  }
}
