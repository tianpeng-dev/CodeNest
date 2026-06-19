<script setup lang="ts">
import type { User } from '@/types/user';

defineProps<{
  user: User;
}>();
</script>

<template>
  <RouterLink class="user-summary-card" :to="`/u/${user.id}`">
    <el-avatar :size="40" :src="user.avatarUrl">
      {{ user.displayName.slice(0, 1) }}
    </el-avatar>
    <div class="user-summary-card__content">
      <strong>{{ user.displayName }}</strong>
      <span>@{{ user.username }}</span>
      <p>{{ user.bio }}</p>
      <dl>
        <div>
          <dt>文章</dt>
          <dd>{{ user.postCount }}</dd>
        </div>
        <div>
          <dt>关注者</dt>
          <dd>{{ user.followerCount }}</dd>
        </div>
      </dl>
    </div>
  </RouterLink>
</template>

<style scoped>
.user-summary-card {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  color: inherit;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.user-summary-card:hover {
  border-color: #a9bdd8;
}

.user-summary-card__content {
  min-width: 0;
}

.user-summary-card strong,
.user-summary-card span,
.user-summary-card p {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-summary-card strong {
  color: #172033;
  font-size: 14px;
}

.user-summary-card span {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.user-summary-card p {
  margin: 8px 0 0;
  color: #475467;
  font-size: 12px;
}

.user-summary-card dl {
  display: flex;
  gap: 14px;
  margin: 10px 0 0;
}

.user-summary-card dt {
  color: #667085;
  font-size: 11px;
}

.user-summary-card dd {
  margin: 2px 0 0;
  color: #172033;
  font-size: 13px;
  font-weight: 800;
}
</style>
