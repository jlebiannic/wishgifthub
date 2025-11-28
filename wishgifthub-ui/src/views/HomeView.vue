<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useAuthStore} from '@/stores/auth'
import {useGroupStore} from '@/stores/group'
import LoginForm from '@/components/LoginForm.vue'
import CreateGroupButton from '@/components/CreateGroupButton.vue'
import GroupCard from '@/components/GroupCard.vue'
import InvitationsDialog from '@/components/InvitationsDialog.vue'

const authStore = useAuthStore()
const groupStore = useGroupStore()

const showInvitationsDialog = ref(false)
const selectedGroupId = ref<string | null>(null)

onMounted(() => {
  // Restaurer la session si elle existe
  authStore.restoreSession()

  // Charger les groupes si l'utilisateur est connecté
  if (authStore.isAuthenticated) {
    loadGroups()
  }
})

async function loadGroups() {
  await groupStore.fetchMyGroups()
}

async function handleLoginSuccess() {
  await loadGroups()
}

async function handleGroupCreated() {
  await loadGroups()
}

async function handleShowInvitations(groupId: string) {
  selectedGroupId.value = groupId
  showInvitationsDialog.value = true
  await groupStore.fetchGroupInvitations(groupId)
}

function handleCloseInvitations() {
  showInvitationsDialog.value = false
  selectedGroupId.value = null
}

function handleLogout() {
  authStore.logout()
  groupStore.groups = []
}
</script>

<template>
  <v-container fluid class="py-8 px-6">
    <!-- Page d'accueil - Non connecté -->
    <div v-if="!authStore.isAuthenticated" class="text-center">
      <v-row justify="center">
        <v-col cols="12" md="8" lg="6">
          <v-icon size="80" color="primary" class="mb-4">mdi-gift-outline</v-icon>
          <h1 class="text-h3 font-weight-bold mb-2">Bienvenue sur WishGiftHub</h1>
          <p class="text-h6 text-medium-emphasis mb-8">
            Gérez vos listes de souhaits et partagez-les avec vos proches
          </p>
        </v-col>
      </v-row>

      <v-row justify="center">
        <v-col cols="12" md="8" lg="6" xl="4">
          <LoginForm @login-success="handleLoginSuccess" />
        </v-col>
      </v-row>
    </div>

    <!-- Dashboard - Connecté -->
    <div v-else>
      <!-- En-tête avec info utilisateur -->
      <v-row class="mb-6">
        <v-col cols="12">
          <v-card elevation="2">
            <v-card-text class="d-flex align-center justify-space-between">
              <div>
                <div class="text-h5 mb-1">
                  <v-icon class="mr-2">mdi-account-circle</v-icon>
                  Bonjour, {{ authStore.user?.username }}
                </div>
                <v-chip
                  v-if="authStore.isAdmin"
                  color="primary"
                  size="small"
                  prepend-icon="mdi-shield-crown"
                  class="mt-2"
                >
                  Administrateur
                </v-chip>
              </div>
              <v-btn
                variant="outlined"
                color="error"
                prepend-icon="mdi-logout"
                @click="handleLogout"
              >
                Déconnexion
              </v-btn>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Bouton créer un groupe (admin uniquement) -->
      <v-row v-if="authStore.isAdmin" class="mb-4">
        <v-col cols="12">
          <CreateGroupButton @group-created="handleGroupCreated" />
        </v-col>
      </v-row>

      <!-- Liste des groupes -->
      <v-row>
        <v-col cols="12">
          <v-card elevation="2">
            <v-card-title class="pa-4">
              <div class="text-h6">
                <v-icon class="mr-2">mdi-account-group</v-icon>
                Mes groupes
              </div>
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-4">
              <!-- Chargement -->
              <v-progress-linear
                v-if="groupStore.isLoading"
                indeterminate
                color="primary"
                class="mb-4"
              />

              <!-- Aucun groupe -->
              <div
                v-else-if="groupStore.groups.length === 0"
                class="text-center py-8"
              >
                <v-icon size="64" color="grey-lighten-1">mdi-account-group-outline</v-icon>
                <div class="text-h6 text-medium-emphasis mt-4">
                  Vous n'appartenez à aucun groupe
                </div>
                <div v-if="authStore.isAdmin" class="text-body-1 text-medium-emphasis mt-2">
                  Créez votre premier groupe pour commencer
                </div>
              </div>

              <!-- Liste des groupes -->
              <div v-else>
                <GroupCard
                  v-for="group in groupStore.groups"
                  :key="group.id"
                  :group="group"
                  :is-admin="authStore.isAdmin"
                  @show-invitations="handleShowInvitations"
                />
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- Dialog des invitations -->
    <v-dialog v-model="showInvitationsDialog" max-width="700">
      <InvitationsDialog
        :invitations="groupStore.invitations"
        :is-loading="groupStore.isLoading"
        @close="handleCloseInvitations"
      />
    </v-dialog>
  </v-container>
</template>


