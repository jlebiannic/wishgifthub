<script setup lang="ts">
import type {WishResponse} from '@/generated/api/wish/data-contracts'
import {useAuthStore} from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'

defineProps<{wish: WishResponse}>()
const authStore = useAuthStore()
</script>

<template>
  <div class="text-body-2" aria-live="polite">
    <div>
      Cadeau commun : {{ wish.participantCount }} / {{ wish.maxParticipants }} participants
      <span v-if="wish.full"> — Complet</span>
    </div>
    <div v-if="wish.participating && wish.userId !== authStore.user?.id" class="text-success">
      Vous participez à ce cadeau
    </div>
    <ul v-if="wish.participants?.length" class="pa-0" style="list-style: none" aria-label="Participants">
      <li v-for="participant in wish.participants" :key="participant.id" class="d-flex align-center mt-1">
        <UserAvatar :avatar-id="participant.avatarId" :size="24" class="mr-2" />
        <span>{{ authStore.getMemberDisplayName(participant) }}</span>
      </li>
    </ul>
  </div>
</template>
