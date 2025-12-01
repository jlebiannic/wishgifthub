<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {useGroupStore} from '@/stores/group'
import {useWishStore} from '@/stores/wish'
import MemberCard from '@/components/MemberCard.vue'
import AddWishDialog from '@/components/AddWishDialog.vue'
import type {UserResponse} from '@/generated/api/wish/data-contracts'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const groupStore = useGroupStore()
const wishStore = useWishStore()

const groupId = ref(route.params.groupId as string)
const groupName = ref('')
const members = ref<UserResponse[]>([])
const showAddWishDialog = ref(false)
const isLoading = ref(true)

// Trier les membres : l'utilisateur connecté en premier
const sortedMembers = computed(() => {
  const currentUserId = authStore.user?.id
  if (!currentUserId) return members.value

  return [...members.value].sort((a, b) => {
    if (a.id === currentUserId) return -1
    if (b.id === currentUserId) return 1
    return 0
  })
})

// Récupérer les souhaits d'un membre spécifique
function getMemberWishes(memberId: string) {
  return wishStore.wishes.filter(w => w.userId === memberId)
}

onMounted(async () => {
  if (!groupId.value) {
    router.push('/')
    return
  }

  // Restaurer la session si elle existe (pour les favoris/rafraîchissement)
  if (!authStore.isAuthenticated) {
    await authStore.restoreSession()
  }

  // Si toujours pas authentifié après restauration, rediriger vers accueil
  if (!authStore.isAuthenticated) {
    router.push('/')
    return
  }

  try {
    // Récupérer les informations du groupe
    // Utiliser fetchMyGroups() pour les non-admins
    if (authStore.isAdmin) {
      await groupStore.fetchGroups()
    } else {
      await groupStore.fetchMyGroups()
    }

    const group = groupStore.groups.find(g => g.id === groupId.value)
    if (group) {
      groupName.value = group.name
    }

    // Récupérer les membres du groupe
    await groupStore.fetchGroupMembers(groupId.value)
    members.value = groupStore.members

    // Récupérer tous les souhaits du groupe
    await wishStore.fetchGroupWishes(groupId.value)
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
    // En cas d'erreur (ex: groupe non accessible), retourner à l'accueil
    router.push('/')
  } finally {
    isLoading.value = false
  }
})

async function handleWishUpdated() {
  // Recharger les souhaits après une mise à jour
  await wishStore.fetchGroupWishes(groupId.value)
}

async function handleWishAdded() {
  // Recharger les souhaits après un ajout
  await wishStore.fetchGroupWishes(groupId.value)
}

function handleAddWish() {
  showAddWishDialog.value = true
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <v-container fluid class="py-8 px-6">
    <!-- En-tête -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-btn
          variant="text"
          prepend-icon="mdi-arrow-left"
          @click="goBack"
          class="mb-4"
        >
          Retour aux groupes
        </v-btn>

        <div class="d-flex align-center">
          <v-icon size="large" color="primary" class="mr-3">mdi-account-group</v-icon>
          <div>
            <h1 class="text-h4 font-weight-bold">{{ groupName || 'Groupe' }}</h1>
            <p class="text-body-1 text-medium-emphasis">
              {{ members.length }} membre{{ members.length > 1 ? 's' : '' }}
            </p>
          </div>
        </div>
      </v-col>
    </v-row>

    <!-- Chargement -->
    <v-row v-if="isLoading" justify="center" class="my-8">
      <v-col cols="12" class="text-center">
        <v-progress-circular
          indeterminate
          color="primary"
          size="64"
          class="mb-4"
        />
        <p class="text-h6">Chargement des membres...</p>
      </v-col>
    </v-row>

    <!-- Liste des membres -->
    <v-row v-else>
      <v-col cols="12">
        <div v-if="sortedMembers.length === 0" class="text-center py-12">
          <v-icon size="80" color="grey-lighten-1">mdi-account-off-outline</v-icon>
          <p class="text-h6 text-medium-emphasis mt-4">
            Aucun membre dans ce groupe
          </p>
        </div>

        <MemberCard
          v-for="member in sortedMembers"
          :key="member.id"
          :member="member"
          :wishes="getMemberWishes(member.id)"
          :group-id="groupId"
          :is-current-user="member.id === authStore.user?.id"
          :all-members="members"
          @add-wish="handleAddWish"
          @wish-updated="handleWishUpdated"
        />
      </v-col>
    </v-row>

    <!-- Dialog d'ajout de souhait -->
    <AddWishDialog
      v-model="showAddWishDialog"
      :group-id="groupId"
      @wish-added="handleWishAdded"
    />
  </v-container>
</template>

