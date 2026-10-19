<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {useGroupStore} from '@/stores/group'
import {useWishStore} from '@/stores/wish'
import MemberCard from '@/components/MemberCard.vue'
import AddWishDialog from '@/components/AddWishDialog.vue'
import ChildrenDialog from '@/components/ChildrenDialog.vue'
import GroupSettingsDialog from '@/components/GroupSettingsDialog.vue'
import {useChildrenStore} from '@/stores/children'
import {useNotificationStore} from '@/stores/notification'
import type {UserResponse} from '@/generated/api/wish/data-contracts'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const groupStore = useGroupStore()
const wishStore = useWishStore()
const childrenStore = useChildrenStore()
const notifications = useNotificationStore()

const groupId = computed(() => route.params.groupId as string)
const groupName = ref('')
const members = ref<UserResponse[]>([])
const showAddWishDialog = ref(false)
const isLoading = ref(true)
const expandedMembers = ref<Map<string, boolean>>(new Map())
const showChildrenDialog = ref(false)
const showSettingsDialog = ref(false)
const currentGroup = computed(() => groupStore.groups.find(group => group.id === groupId.value))
const refreshing = ref(false)
const selectedUserId = ref('')
const managedChildren = computed(() => childrenStore.currentGroupId === groupId.value ? childrenStore.children : [])
const wishOwners = computed(() => [
  {title: 'Moi', value: authStore.user?.id ?? ''},
  ...managedChildren.value.map(child => ({title: authStore.getMemberDisplayName(child), value: child.id}))
])
const selectedOwnerName = computed(() => wishOwners.value.find(owner => owner.value === selectedUserId.value)?.title)
const selectedChildId = computed(() => selectedUserId.value === authStore.user?.id ? undefined : selectedUserId.value)

watch(wishOwners, owners => {
  if (!owners.some(owner => owner.value === selectedUserId.value)) {
    selectedUserId.value = authStore.user?.id ?? ''
    showAddWishDialog.value = false
  }
}, {immediate: true})

const sortedMembers = computed(() => {
  const memberIds = new Set(members.value.map(member => member.id))
  const childrenByParent = new Map<string, UserResponse[]>()
  const parents = members.value.filter(member => {
    if (member.parentId && memberIds.has(member.parentId)) {
      const children = childrenByParent.get(member.parentId) ?? []
      children.push(member)
      childrenByParent.set(member.parentId, children)
      return false
    }
    return true
  })
  parents.sort((a, b) => Number(b.id === authStore.user?.id) - Number(a.id === authStore.user?.id))
  return parents.flatMap(parent => [parent, ...(childrenByParent.get(parent.id) ?? [])])
})

// Récupérer les souhaits d'un membre spécifique
function getMemberWishes(memberId: string) {
  return wishStore.wishes.filter(w => w.userId === memberId)
}

// Vérifier si un membre est déplié
function isMemberExpanded(memberId: string): boolean {
  return expandedMembers.value.get(memberId) ?? false
}

// Gérer le changement d'expansion d'un membre
function handleExpansionChanged(memberId: string, expanded: boolean) {
  expandedMembers.value.set(memberId, expanded)
}

// Tout déplier
function expandAll() {
  sortedMembers.value.forEach(member => {
    expandedMembers.value.set(member.id, true)
  })
  // Forcer la réactivité
  expandedMembers.value = new Map(expandedMembers.value)
}

// Tout replier
function collapseAll() {
  sortedMembers.value.forEach(member => {
    expandedMembers.value.set(member.id, false)
  })
  // Forcer la réactivité
  expandedMembers.value = new Map(expandedMembers.value)
}

async function loadGroup() {
  const requestedGroupId = groupId.value
  isLoading.value = true
  showAddWishDialog.value = false
  showChildrenDialog.value = false
  showSettingsDialog.value = false
  selectedUserId.value = authStore.user?.id ?? ''
  members.value = []
  expandedMembers.value = new Map()
  childrenStore.reset()
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
    // Inclure les groupes rejoints par invitation, même pour un administrateur.
    await groupStore.fetchMyGroups()

    const group = groupStore.groups.find(g => g.id === groupId.value)
    if (group) {
      groupName.value = group.name
    }

    // Récupérer les membres du groupe
    const [loadedMembers] = await Promise.all([
      groupStore.fetchGroupMembers(requestedGroupId),
      wishStore.fetchGroupWishes(requestedGroupId),
      childrenStore.fetchChildren(requestedGroupId)
    ])
    if (groupId.value !== requestedGroupId) return
    members.value = loadedMembers

    // Si un hash est présent dans l'URL, déplier le membre correspondant
    if (route.hash) {
      const memberId = route.hash.replace('#member-', '')
      if (memberId) {
        expandedMembers.value.set(memberId, true)
        // Scroll vers le membre après un court délai pour laisser le temps au rendu
        setTimeout(() => {
          const element = document.getElementById(`member-${memberId}`)
          if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'start' })
          }
        }, 300)
      }
    }
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
    notifications.showError('Impossible de charger les membres, les souhaits ou les profils enfants')
    // En cas d'erreur (ex: groupe non accessible), retourner à l'accueil
    router.push('/')
  } finally {
    if (groupId.value === requestedGroupId) isLoading.value = false
  }
}

watch(groupId, loadGroup, {immediate: true})

async function handleChildrenChanged() {
  const requestedGroupId = groupId.value
  refreshing.value = true
  try {
    const [loadedMembers] = await Promise.all([
      groupStore.fetchGroupMembers(requestedGroupId),
      wishStore.fetchGroupWishes(requestedGroupId),
      childrenStore.fetchChildren(requestedGroupId)
    ])
    if (groupId.value !== requestedGroupId) return
    members.value = loadedMembers
    expandedMembers.value = new Map([...expandedMembers.value].filter(([id]) => members.value.some(member => member.id === id)))
  } catch {
    notifications.showError('Le profil est enregistré, mais le groupe n’a pas pu être actualisé. Veuillez recharger la page.')
  } finally {
    refreshing.value = false
  }
}

async function handleWishUpdated() {
  // Recharger les souhaits après une mise à jour
  try {
    await wishStore.fetchGroupWishes(groupId.value)
  } catch {
    notifications.showError('Impossible d’actualiser les souhaits du groupe')
  }
}

async function handleWishAdded() {
  // Recharger les souhaits après un ajout
  await handleWishUpdated()
}

function handleAddWish(memberId: string) {
  if (!wishOwners.value.some(owner => owner.value === memberId)) return
  selectedUserId.value = memberId
  showAddWishDialog.value = true
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <v-container fluid class="py-8 px-6 group-container">
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

        <div class="d-flex flex-column flex-sm-row align-start align-sm-center">
          <div class="d-flex align-center mb-3 mb-sm-0">
            <v-icon size="large" color="primary" class="mr-3">mdi-account-group</v-icon>
            <div class="flex-grow-1">
              <h1 class="text-h4 font-weight-bold">{{ groupName || 'Groupe' }}</h1>
              <p class="text-body-1 text-medium-emphasis">
                {{ members.length }} membre{{ members.length > 1 ? 's' : '' }}
              </p>
            </div>
          </div>

          <!-- Boutons d'actions -->
          <div v-if="members.length > 0" class="ml-sm-auto d-flex align-center gap-2">
            <v-btn
              color="primary"
              variant="tonal"
              size="small"
              prepend-icon="mdi-view-dashboard"
              @click="router.push(`/group/${groupId}/dashboard`)"
            >
              <span class="d-none d-sm-inline">Mon tableau de bord</span>
              <span class="d-inline d-sm-none">Dashboard</span>
            </v-btn>
            <v-btn
              variant="outlined"
              size="small"
              prepend-icon="mdi-chevron-down"
              @click="expandAll"
            >
              <span class="d-none d-sm-inline">Tout déplier</span>
              <span class="d-inline d-sm-none">Déplier</span>
            </v-btn>
            <v-btn
              variant="outlined"
              size="small"
              prepend-icon="mdi-chevron-up"
              @click="collapseAll"
            >
              <span class="d-none d-sm-inline">Tout replier</span>
              <span class="d-inline d-sm-none">Replier</span>
            </v-btn>
          </div>
        </div>
      </v-col>
    </v-row>

    <v-row v-if="!isLoading">
      <v-col cols="12" class="d-flex flex-wrap align-center ga-3">
        <v-btn v-if="currentGroup?.adminId === authStore.user?.id"
               prepend-icon="mdi-cog" variant="tonal" @click="showSettingsDialog = true">
          Paramètres du groupe
        </v-btn>
        <v-btn prepend-icon="mdi-account-child" variant="tonal" :disabled="refreshing || showAddWishDialog"
               @click="showChildrenDialog = true">
          Gérer mes enfants
        </v-btn>
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
          :id="`member-${member.id}`"
          :class="{'child-member': member.parentId && members.some(parent => parent.id === member.parentId)}"
          :member="member"
          :wishes="getMemberWishes(member.id)"
          :group-id="groupId"
          :is-current-user="member.id === authStore.user?.id"
          :is-managed-child="managedChildren.some(child => child.id === member.id)"
          :all-members="members"
          :initially-expanded="isMemberExpanded(member.id)"
          @add-wish="handleAddWish"
          @wish-updated="handleWishUpdated"
          @expansion-changed="handleExpansionChanged"
        />
      </v-col>
    </v-row>

    <!-- Dialog d'ajout de souhait -->
    <AddWishDialog
      v-model="showAddWishDialog"
      :group-id="groupId"
      :target-user-id="selectedChildId"
      :target-name="selectedOwnerName"
      @wish-added="handleWishAdded"
    />
    <ChildrenDialog v-model="showChildrenDialog" :group-id="groupId" :refreshing="refreshing"
                    @changed="handleChildrenChanged" />
    <GroupSettingsDialog v-if="currentGroup && currentGroup.adminId === authStore.user?.id"
                         v-model="showSettingsDialog" :group="currentGroup" />
  </v-container>
</template>

<style scoped>
.group-container {
  max-width: 1600px;
  margin: 0 auto;
}

.child-member {
  margin-inline-start: 24px;
}

@media (min-width: 600px) {
  .child-member {
    margin-inline-start: 48px;
  }
}
</style>
