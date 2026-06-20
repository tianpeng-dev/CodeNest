<script setup lang="ts">
import { useAuth } from '@clerk/vue';
import { watch } from 'vue';
import { useAuthStore } from '@/stores/auth.store';
import { setAuthTokenProvider } from '@/services/http';

const authStore = useAuthStore();
const { getToken, isLoaded, isSignedIn } = useAuth();

setAuthTokenProvider(() => getToken.value?.() ?? null);

watch(
  [isLoaded, isSignedIn],
  async ([loaded, signedIn]) => {
    if (!loaded) {
      return;
    }

    if (signedIn) {
      try {
        await authStore.loadCurrentUser();
      } catch {
        authStore.clearAuth();
      }
      return;
    }

    if (!window.localStorage.getItem('codenest_token')) {
      authStore.clearAuth();
    }
  },
  { immediate: true },
);
</script>

<template>
  <RouterView />
</template>
