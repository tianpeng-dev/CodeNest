import { defineStore } from 'pinia';
import type { ID } from '@/types/common';

type InteractionMap = Record<string, boolean>;

function toggleRecord(record: InteractionMap, id: ID) {
  if (record[id]) {
    delete record[id];
  } else {
    record[id] = true;
  }
}

export const useInteractionStore = defineStore('interaction', {
  state: () => ({
    likedIds: {} as InteractionMap,
    dislikedIds: {} as InteractionMap,
    favoritedIds: {} as InteractionMap,
    followedIds: {} as InteractionMap,
  }),
  actions: {
    toggleLike(id: ID) {
      toggleRecord(this.likedIds, id);

      if (this.likedIds[id]) {
        delete this.dislikedIds[id];
      }
    },
    toggleDislike(id: ID) {
      toggleRecord(this.dislikedIds, id);

      if (this.dislikedIds[id]) {
        delete this.likedIds[id];
      }
    },
    toggleFavorite(id: ID) {
      toggleRecord(this.favoritedIds, id);
    },
    toggleFollow(id: ID) {
      toggleRecord(this.followedIds, id);
    },
  },
});
