<script setup lang="ts">
import {useRouter} from 'vue-router'
import type {Group} from '@/stores/group'

const router = useRouter()

const props = defineProps<{
  group: Group
  isAdmin: boolean
}>()

const emit = defineEmits<{
  showMembers: [groupId: string]
}>()

function handleCardClick() {
  // Naviguer vers la page du groupe
  router.push(`/group/${props.group.id}`)
}

function handleShowMembers(event: Event) {
  event.stopPropagation()
  emit('showMembers', props.group.id)
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<template>
  <v-card elevation="2" class="mb-3" hover @click="handleCardClick" style="cursor: pointer">
    <v-card-text class="d-flex align-center justify-space-between">
      <div class="flex-grow-1">
        <div class="d-flex align-center mb-1">
          <v-icon color="primary" class="mr-2">mdi-gift-outline</v-icon>
          <div class="text-h6">{{ group.name }}</div>
        </div>
        <div class="text-body-2 text-medium-emphasis">
          <v-chip size="x-small" color="secondary" class="mr-2">
            {{ group.type }}
          </v-chip>
          Créé le {{ formatDate(group.createdAt) }}
        </div>
      </div>

      <!-- Icône voir les membres (admin uniquement) -->
      <v-tooltip v-if="isAdmin" text="Gérer les invitations" location="left">
        <template v-slot:activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            v-bind="tooltipProps"
            @click="handleShowMembers"
          >
            <v-icon>mdi-account-multiple</v-icon>
          </v-btn>
        </template>
      </v-tooltip>
    </v-card-text>
  </v-card>
</template>

