<script setup lang="ts">
import {ref, watch} from 'vue'
import type {UserResponse, WishResponse} from '@/generated/api/wish/data-contracts'
import {useAuthStore} from '@/stores/auth'
import {useWishStore} from '@/stores/wish'
import {useNotificationStore} from '@/stores/notification'
import {useConfirmStore} from '@/stores/confirm'
import EditWishDialog from './EditWishDialog.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{
  member: UserResponse
  wishes: WishResponse[]
  groupId: string
  isCurrentUser: boolean
  initiallyExpanded?: boolean
  allMembers: UserResponse[]
}>()

const emit = defineEmits<{
  addWish: []
  wishUpdated: []
  expansionChanged: [memberId: string, expanded: boolean]
}>()

const authStore = useAuthStore()
const wishStore = useWishStore()
const notificationStore = useNotificationStore()
const confirmStore = useConfirmStore()

const expanded = ref(props.initiallyExpanded || false)
const isReserving = ref<string | null>(null)
const isDeleting = ref<string | null>(null)
const editingWish = ref<WishResponse | null>(null)
const showEditDialog = ref(false)

// Surveiller les changements de la prop initiallyExpanded (pour tout déplier/replier)
watch(() => props.initiallyExpanded, (newValue) => {
  if (newValue !== undefined) {
    expanded.value = newValue
  }
})

// Émettre l'événement quand l'état d'expansion change
watch(expanded, (newValue) => {
  emit('expansionChanged', props.member.id, newValue)
})

// Récupérer les informations de l'utilisateur qui a réservé
function getReservedByName(wish: WishResponse): string {
  if (!wish.reservedBy) return ''

  // Si c'est moi qui ai réservé
  if (wish.reservedBy === authStore.user?.id) {
    return 'Réservé par moi'
  }

  // Chercher la personne dans la liste des membres
  const reserver = props.allMembers.find(m => m.id === wish.reservedBy)
  if (reserver) {
    return `Réservé par ${authStore.getMemberDisplayName(reserver)}`
  }

  // Fallback si on ne trouve pas (ne devrait pas arriver)
  return 'Réservé'
}

// Version courte pour mobile
function getReservedByNameShort(wish: WishResponse): string {
  if (!wish.reservedBy) return ''

  // Si c'est moi qui ai réservé
  if (wish.reservedBy === authStore.user?.id) {
    return 'Moi'
  }

  // Chercher la personne dans la liste des membres
  const reserver = props.allMembers.find(m => m.id === wish.reservedBy)
  if (reserver) {
    return authStore.getMemberDisplayName(reserver)
  }

  return 'Réservé'
}

// Vérifier si je peux réserver ce souhait
function canReserve(wish: WishResponse): boolean {
  // Je ne peux pas réserver mes propres souhaits
  if (wish.userId === authStore.user?.id) {
    return false
  }

  // Je ne peux pas réserver un souhait déjà réservé (par moi ou quelqu'un d'autre)
  if (wish.reservedBy) {
    return false
  }

  return true
}

// Vérifier si je peux annuler la réservation
function canUnreserve(wish: WishResponse): boolean {
  return wish.reservedBy === authStore.user?.id
}

async function handleReserve(wish: WishResponse) {
  isReserving.value = wish.id

  try {
    await wishStore.reserveWish(props.groupId, wish.id)
    notificationStore.showSuccess('Souhait réservé avec succès !')
    emit('wishUpdated')
  } catch (error: any) {
    console.error('Erreur lors de la réservation:', error)

    const errorMessage = error.response?.data?.message || error.message || ''

    if (errorMessage.includes('déjà réservé') || errorMessage.includes('already reserved') || error.response?.status === 409) {
      notificationStore.showWarning('Ce souhait a déjà été réservé par quelqu\'un d\'autre')
      emit('wishUpdated')
    } else {
      notificationStore.showError('Erreur lors de la réservation du souhait')
    }
  } finally {
    isReserving.value = null
  }
}

async function handleUnreserve(wish: WishResponse) {
  isReserving.value = wish.id

  try {
    await wishStore.unreserveWish(props.groupId, wish.id)
    notificationStore.showSuccess('Réservation annulée')
    emit('wishUpdated')
  } catch (error: any) {
    console.error('Erreur lors de l\'annulation:', error)

    const errorMessage = error.response?.data?.message || error.message || ''

    if (errorMessage.includes('réservé') || error.response?.status === 409) {
      notificationStore.showWarning('Ce souhait a été modifié entre temps')
      emit('wishUpdated')
    } else {
      notificationStore.showError('Erreur lors de l\'annulation de la réservation')
    }
  } finally {
    isReserving.value = null
  }
}

async function handleDelete(wish: WishResponse) {
  const confirmed = await confirmStore.confirmDelete('ce souhait')
  if (!confirmed) {
    return
  }

  isDeleting.value = wish.id

  try {
    await wishStore.deleteWish(props.groupId, wish.id)
    notificationStore.showSuccess('Souhait supprimé')
    emit('wishUpdated')
  } catch (error: any) {
    console.error('Erreur lors de la suppression:', error)
    notificationStore.showError(error.response?.data?.message || 'Erreur lors de la suppression du souhait')
  } finally {
    isDeleting.value = null
  }
}

function handleEdit(wish: WishResponse) {
  editingWish.value = wish
  showEditDialog.value = true
}

function handleWishUpdated() {
  showEditDialog.value = false
  editingWish.value = null
  emit('wishUpdated')
}

function toggleExpand() {
  expanded.value = !expanded.value
}
</script>

<template>
  <v-card
    :elevation="expanded ? 8 : 2"
    class="mb-3"
    :class="{'border-primary': isCurrentUser}"
  >
    <v-card-text
      class="d-flex align-center pa-4 cursor-pointer flex-wrap"
      @click="toggleExpand"
    >
      <!-- Avatar -->
      <UserAvatar
        :avatar-id="member.avatarId"
        :size="56"
        class="mr-4 flex-shrink-0"
      />

      <!-- Nom du membre -->
      <div class="flex-grow-1" style="min-width: 150px;">
        <div class="text-h6">
          <span v-if="isCurrentUser" class="font-weight-bold">Moi</span>
          <span v-if="isCurrentUser"> ({{ authStore.getMemberDisplayName(member) }})</span>
          <span v-else>{{ authStore.getMemberDisplayName(member) }}</span>
        </div>
        <div class="text-caption text-medium-emphasis">
          {{ wishes.length }} souhait{{ wishes.length > 1 ? 's' : '' }}
        </div>
      </div>

      <!-- Bouton Ajouter (seulement pour l'utilisateur connecté) -->
      <!-- Sur mobile : passe en dessous grâce au flex-wrap -->
      <v-btn
        v-if="isCurrentUser"
        color="primary"
        variant="elevated"
        prepend-icon="mdi-plus"
        @click.stop="emit('addWish')"
        size="small"
        class="mr-2 mt-2 mt-sm-0"
      >
        <span class="d-none d-sm-inline">Ajouter un souhait</span>
        <span class="d-inline d-sm-none">Ajouter</span>
      </v-btn>

      <!-- Icône expand -->
      <v-icon class="flex-shrink-0">
        {{ expanded ? 'mdi-chevron-up' : 'mdi-chevron-down' }}
      </v-icon>
    </v-card-text>

    <!-- Liste des souhaits (visible quand expanded) -->
    <v-expand-transition>
      <div v-show="expanded">
        <v-divider />
        <v-card-text class="pa-4">
          <!-- Aucun souhait -->
          <div v-if="wishes.length === 0" class="text-center py-8">
            <v-icon size="64" color="grey-lighten-1">mdi-gift-off-outline</v-icon>
            <div class="text-body-1 text-medium-emphasis mt-2">
              Aucun souhait pour le moment
            </div>
          </div>

          <!-- Liste des souhaits -->
          <v-row v-else>
            <v-col
              v-for="wish in wishes"
              :key="wish.id"
              cols="12"
              sm="6"
              md="4"
            >
              <v-card variant="outlined" class="h-100">
                <!-- Image du souhait (si disponible) -->
                <v-img
                  v-if="wish.imageUrl"
                  :src="wish.imageUrl"
                  height="150"
                  cover
                >
                  <template v-slot:placeholder>
                    <v-row
                      class="fill-height ma-0"
                      align="center"
                      justify="center"
                    >
                      <v-icon size="64" color="grey-lighten-1">
                        mdi-gift-outline
                      </v-icon>
                    </v-row>
                  </template>
                </v-img>
                <div
                  v-else
                  class="d-flex align-center justify-center bg-grey-lighten-3"
                  style="height: 150px"
                >
                  <v-icon size="64" color="grey">mdi-gift-outline</v-icon>
                </div>

                <v-card-text class="pb-0">
                  <!-- Titre -->
                  <div class="text-subtitle-1 font-weight-bold mb-2">
                    {{ wish.giftName }}
                  </div>

                  <!-- Prix (si disponible) -->
                  <div v-if="wish.price" class="text-h6 text-primary mb-2">
                    {{ wish.price }}
                  </div>

                  <!-- Description -->
                  <div
                    v-if="wish.description"
                    class="text-body-2 text-medium-emphasis mb-2"
                    style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;"
                  >
                    {{ wish.description }}
                  </div>

                  <!-- URL -->
                  <v-btn
                    v-if="wish.url"
                    :href="wish.url"
                    target="_blank"
                    variant="text"
                    size="small"
                    prepend-icon="mdi-open-in-new"
                    class="mb-2 px-0"
                  >
                    Voir le produit
                  </v-btn>
                </v-card-text>

                <!-- Statut de réservation et actions -->
                <v-card-actions class="pt-0">
                  <v-chip
                    v-if="wish.reservedBy"
                    :color="wish.reservedBy === authStore.user?.id ? 'success' : 'warning'"
                    size="small"
                    variant="flat"
                  >
                    <v-icon start size="small">
                      {{ wish.reservedBy === authStore.user?.id ? 'mdi-check' : 'mdi-lock' }}
                    </v-icon>
                    <!-- Texte complet sur desktop -->
                    <span class="d-none d-sm-inline">{{ getReservedByName(wish) }}</span>
                    <!-- Texte court sur mobile -->
                    <span class="d-inline d-sm-none">{{ getReservedByNameShort(wish) }}</span>
                  </v-chip>

                  <v-spacer />

                  <!-- Bouton Modifier (uniquement pour mes propres souhaits non réservés) -->
                  <v-btn
                    v-if="isCurrentUser && !wish.reservedBy"
                    color="primary"
                    size="small"
                    variant="text"
                    icon="mdi-pencil"
                    @click="handleEdit(wish)"
                    class="mr-2"
                  >
                    <v-icon>mdi-pencil</v-icon>
                    <v-tooltip activator="parent" location="top">
                      Modifier
                    </v-tooltip>
                  </v-btn>

                  <!-- Bouton Supprimer (uniquement pour mes propres souhaits) -->
                  <v-btn
                    v-if="isCurrentUser"
                    color="error"
                    size="small"
                    variant="text"
                    icon="mdi-delete"
                    @click="handleDelete(wish)"
                    :loading="isDeleting === wish.id"
                  >
                    <v-icon>mdi-delete</v-icon>
                    <v-tooltip activator="parent" location="top">
                      Supprimer
                    </v-tooltip>
                  </v-btn>

                  <!-- Bouton Réserver -->
                  <v-btn
                    v-if="canReserve(wish)"
                    color="primary"
                    size="small"
                    variant="elevated"
                    prepend-icon="mdi-gift"
                    @click="handleReserve(wish)"
                    :loading="isReserving === wish.id"
                  >
                    Réserver
                  </v-btn>

                  <!-- Bouton Annuler réservation -->
                  <v-btn
                    v-if="canUnreserve(wish)"
                    color="warning"
                    size="small"
                    variant="elevated"
                    prepend-icon="mdi-cancel"
                    @click="handleUnreserve(wish)"
                    :loading="isReserving === wish.id"
                  >
                    Annuler la réservation
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-col>
          </v-row>
        </v-card-text>
      </div>
    </v-expand-transition>
  </v-card>

  <!-- Dialogue de modification -->
  <EditWishDialog
    v-model="showEditDialog"
    :group-id="groupId"
    :wish="editingWish"
    @wish-updated="handleWishUpdated"
  />
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
</style>

