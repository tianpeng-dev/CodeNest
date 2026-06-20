<script setup lang="ts">
import { SignUp } from '@clerk/vue';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const redirectTarget = computed(() => {
  const redirect = String(route.query.redirect ?? '');
  return redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/creator/overview';
});
</script>

<template>
  <div class="clerk-auth-page">
    <SignUp
      routing="path"
      path="/sign-up"
      sign-in-url="/sign-in"
      :fallback-redirect-url="redirectTarget"
      :force-redirect-url="redirectTarget"
    />
  </div>
</template>

<style scoped>
.clerk-auth-page {
  display: flex;
  justify-content: center;
  padding: 48px 0 72px;
}
</style>
