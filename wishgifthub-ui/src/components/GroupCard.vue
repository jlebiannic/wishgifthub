<script setup lang="ts">
import type {Group} from '@/stores/group'

const props = defineProps<{
  group: Group
  isAdmin: boolean
}>()

const emit = defineEmits<{
  showInvitations: [groupId: string]
}>()

function handleShowInvitations() {
  emit('showInvitations', props.group.id)
}
</script>

<template>
  <v-card elevation="2" class="mb-3" hover>
    <v-card-text class="d-flex align-center justify-space-between">
      <div class="flex-grow-1">
        <div class="text-h6 mb-1">{{ group.name }}</div>
        <div v-if="group.description" class="text-body-2 text-medium-emphasis">
          {{ group.description }}
        </div>
      </div>

      <v-tooltip v-if="isAdmin" text="Voir les invitations" location="left">
        <template v-slot:activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            v-bind="tooltipProps"
            @click="handleShowInvitations"
          >
            <v-icon>mdi-eye</v-icon>
          </v-btn>
        </template>
      </v-tooltip>
    </v-card-text>
  </v-card>
</template>

