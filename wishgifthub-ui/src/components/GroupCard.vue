<script setup lang="ts">
import type {Group} from '@/stores/group'

const props = defineProps<{
  group: Group
  isAdmin: boolean
}>()

const emit = defineEmits<{
  showMembers: [groupId: string]
}>()

function handleShowMembers() {
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
  <v-card elevation="2" class="mb-3" hover>
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

      <v-tooltip text="Voir les membres" location="left">
        <template v-slot:activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            v-bind="tooltipProps"
            @click="handleShowMembers"
          >
            <v-icon>mdi-account-group</v-icon>
          </v-btn>
        </template>
      </v-tooltip>
    </v-card-text>
  </v-card>
</template>

