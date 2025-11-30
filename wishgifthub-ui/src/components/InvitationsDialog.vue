<script setup lang="ts">
import type {GroupMember} from '@/stores/group'

defineProps<{
  members: GroupMember[]
  isLoading: boolean
}>()

defineEmits<{
  close: []
}>()
</script>

<template>
  <v-card>
    <v-card-title class="d-flex align-center justify-space-between pa-4">
      <div class="text-h6">
        <v-icon class="mr-2">mdi-account-multiple</v-icon>
        Membres du groupe
      </div>
    </v-card-title>

    <v-divider />

    <v-card-text class="pa-4" style="max-height: 500px">
      <v-progress-linear v-if="isLoading" indeterminate color="primary" class="mb-4" />

      <div v-if="!isLoading && members.length === 0" class="text-center py-8">
        <v-icon size="64" color="grey-lighten-1">mdi-account-outline</v-icon>
        <div class="text-h6 text-medium-emphasis mt-4">Aucun membre</div>
      </div>

      <v-list v-else lines="two">
        <v-list-item
          v-for="member in members"
          :key="member.id"
          class="mb-2 rounded border"
        >
          <template v-slot:prepend>
            <v-avatar :color="member.isAdmin ? 'primary' : 'grey'">
              <v-icon
                :icon="member.isAdmin ? 'mdi-shield-crown' : 'mdi-account'"
                color="white"
              />
            </v-avatar>
          </template>

          <v-list-item-title>
            {{ member.email }}
          </v-list-item-title>

          <v-list-item-subtitle>
            Membre depuis {{ new Date(member.createdAt).toLocaleDateString('fr-FR') }}
          </v-list-item-subtitle>

          <template v-slot:append>
            <v-chip
              v-if="member.isAdmin"
              color="primary"
              size="small"
              variant="flat"
            >
              Administrateur
            </v-chip>
            <v-chip
              v-else
              color="grey"
              size="small"
              variant="flat"
            >
              Membre
            </v-chip>
          </template>
        </v-list-item>
      </v-list>
    </v-card-text>

    <v-divider />

    <v-card-actions class="pa-4">
      <v-spacer />
      <v-btn color="primary" variant="text" @click="$emit('close')">
        Fermer
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

