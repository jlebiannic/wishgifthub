<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {useGroupStore} from '@/stores/group'
import LoginForm from '@/components/LoginForm.vue'
import CreateGroupButton from '@/components/CreateGroupButton.vue'
import GroupCard from '@/components/GroupCard.vue'
import InvitationsDialog from '@/components/InvitationsDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const groupStore = useGroupStore()

const showMembersDialog = ref(false)
const selectedGroupId = ref<string | null>(null)
const showExpiredAlert = ref(false)

const isTokenExpired = computed(() => route.query.expired === 'true')

onMounted(async () => {
  // Restaurer la session si elle existe
  await authStore.restoreSession()

  // Si l'utilisateur est authentifié et qu'il n'y a pas de message d'expiration
  // alors on reste sur cette page qui affiche ses groupes
  if (authStore.isAuthenticated && !isTokenExpired.value) {
    // Charger les groupes
    await loadGroups()
  } else if (isTokenExpired.value) {
    // Afficher l'alerte si le token a expiré
    showExpiredAlert.value = true
  }
})

async function loadGroups() {
  await groupStore.fetchMyGroups()
}

async function handleLoginSuccess() {
  // Les groupes sont déjà chargés automatiquement par le store auth lors du login
  // Recharger les groupes pour afficher le dashboard
  await loadGroups()
}

async function handleGroupCreated() {
  await loadGroups()
}

async function handleShowMembers(groupId: string) {
  selectedGroupId.value = groupId
  showMembersDialog.value = true
  await groupStore.fetchGroupMembers(groupId)
  await groupStore.fetchGroupInvitations(groupId)
}

function handleCloseMembers() {
  showMembersDialog.value = false
  selectedGroupId.value = null
}

async function handleInvitationSent() {
  // L'invitation a déjà été ajoutée au store par inviteUser()
  // On recharge uniquement les membres au cas où l'utilisateur invité existe déjà
  if (selectedGroupId.value) {
    await groupStore.fetchGroupMembers(selectedGroupId.value)
  }
}

function handleLogout() {
  authStore.logout()
  groupStore.groups = []
}
</script>

<template>
  <v-container fluid class="py-8 px-6" style="min-height: 100vh;">
    <!-- Page d'accueil - Non connecté -->
    <div v-if="!authStore.isAuthenticated">
      <v-row justify="center">
        <v-col cols="12" md="8" lg="6" class="text-center">
          <!-- Alerte de session expirée -->
          <v-alert
            v-if="showExpiredAlert"
            type="warning"
            variant="tonal"
            closable
            class="mb-6"
            @click:close="showExpiredAlert = false"
          >
            <div class="text-h6 mb-1">
              <v-icon class="mr-2">mdi-clock-alert-outline</v-icon>
              Session expirée
            </div>
            <div class="text-body-1">
              Pour des raisons de sécurité, votre session a expiré. Veuillez vous reconnecter.
            </div>
          </v-alert>

          <v-icon size="80" color="primary" class="mb-4">mdi-gift-outline</v-icon>
          <h1 class="text-h3 font-weight-bold mb-2">Bienvenue sur WishGiftHub</h1>
          <p class="text-h6 text-medium-emphasis mb-8">
            Gérez vos listes de souhaits et partagez-les avec vos proches
          </p>
        </v-col>
      </v-row>

      <v-row justify="center">
        <v-col cols="12" md="8" lg="6" xl="4">
          <!-- Information pour les utilisateurs non-admin -->
          <v-card class="mb-6" variant="outlined">
            <v-card-text class="pa-6">
              <div class="d-flex align-start">
                <v-icon color="info" size="large" class="mr-3 mt-1">
                  mdi-information-outline
                </v-icon>
                <div>
                  <div class="text-subtitle-1 font-weight-bold mb-2">
                    Vous êtes un utilisateur invité ?
                  </div>
                  <div class="text-body-2 text-medium-emphasis">
                    Si vous avez reçu une invitation par email, utilisez le lien fourni dans l'email
                    pour accéder directement à votre groupe.
                  </div>
                  <div class="text-body-2 text-medium-emphasis mt-2">
                    <v-icon size="small" class="mr-1">mdi-lock</v-icon>
                    Ce formulaire de connexion est réservé aux administrateurs.
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>

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
                  @show-members="handleShowMembers"
                />
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- Dialog des membres et invitations -->
    <v-dialog v-model="showMembersDialog" max-width="900">
      <InvitationsDialog
        v-if="selectedGroupId"
        :group-id="selectedGroupId"
        :members="groupStore.members"
        :invitations="groupStore.invitations"
        :is-admin="authStore.isAdmin"
        :is-loading="groupStore.isLoading"
        @close="handleCloseMembers"
        @invitation-sent="handleInvitationSent"
      />
    </v-dialog>
  </v-container>
</template>


