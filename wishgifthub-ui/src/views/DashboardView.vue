<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {useWishStore} from '@/stores/wish'
import {useGroupStore} from '@/stores/group'
import type {WishResponse} from '@/generated/api/wish/data-contracts'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const wishStore = useWishStore()
const groupStore = useGroupStore()

const isLoading = ref(true)
const myWishes = ref<WishResponse[]>([])
const allMembers = computed(() => groupStore.members)
const groupId = computed(() => route.params.groupId as string)
const currentGroup = computed(() => groupStore.groups.find(g => g.id === groupId.value))
const myWishesExpanded = ref(true) // État replié/déplié de "Mes souhaits"

onMounted(async () => {
  try {
    // Charger mes souhaits du groupe actuel
    myWishes.value = await wishStore.fetchMyWishes(groupId.value)

    // Charger tous les souhaits du groupe pour voir mes réservations
    await wishStore.fetchGroupWishes(groupId.value)

    // Charger les membres du groupe pour afficher qui a réservé
    await groupStore.fetchGroupMembers(groupId.value)
  } finally {
    isLoading.value = false
  }
})

// Mes souhaits réservés (par quelqu'un d'autre)
const myReservedWishes = computed(() => {
  return myWishes.value.filter(wish => wish.reservedBy && wish.reservedBy !== authStore.user?.id)
})

// Mes souhaits non réservés
const myUnreservedWishes = computed(() => {
  return myWishes.value.filter(wish => !wish.reservedBy)
})

// Résumé des autres membres avec leurs souhaits
const otherMembersSummary = computed(() => {
  return allMembers.value
    .filter(member => member.id !== authStore.user?.id)
    .map(member => {
      const memberWishes = wishStore.wishes.filter(wish => wish.userId === member.id)
      const reservedWishes = memberWishes.filter(wish => wish.reservedBy)
      const unreservedWishes = memberWishes.filter(wish => !wish.reservedBy)

      // Détail des réservations avec le nom du réserveur
      const reservationDetails = reservedWishes.map(wish => ({
        wish,
        reservedByName: getReserverName(wish.reservedBy!),
        reservedByAvatar: getReserverAvatar(wish.reservedBy!)
      }))

      return {
        member,
        totalWishes: memberWishes.length,
        reservedCount: reservedWishes.length,
        unreservedCount: unreservedWishes.length,
        reservationDetails
      }
    })
    .filter(summary => summary.totalWishes > 0) // Ne montrer que les membres avec au moins un souhait
})

// Récupérer le nom de la personne qui a réservé
function getReserverName(reservedBy: string): string {
  const member = allMembers.value.find(m => m.id === reservedBy)
  return member ? authStore.getMemberDisplayName(member) : 'Inconnu'
}

// Récupérer l'avatar de la personne qui a réservé
function getReserverAvatar(reservedBy: string): string | null | undefined {
  const member = allMembers.value.find(m => m.id === reservedBy)
  return member?.avatarId
}

// Formater le prix
function formatPrice(price: string | number | null | undefined): string {
  if (!price) return ''
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  return numPrice.toFixed(2)
}

// Naviguer vers la page du groupe avec le membre sélectionné
function goToMember(memberId: string) {
  router.push({
    path: `/group/${groupId.value}`,
    hash: `#member-${memberId}`
  })
}
</script>

<template>
  <v-container fluid class="pa-4 dashboard-container">
    <v-row>
      <v-col cols="12">
        <!-- En-tête -->
        <div class="d-flex align-center mb-6">
          <v-btn
            icon
            variant="text"
            class="mr-3"
            @click="router.push(`/group/${groupId}`)"
          >
            <v-icon>mdi-arrow-left</v-icon>
          </v-btn>
          <v-icon size="large" class="mr-3" color="primary">mdi-view-dashboard</v-icon>
          <div>
            <h1 class="text-h4">Mon tableau de bord</h1>
            <div v-if="currentGroup" class="text-subtitle-1 text-medium-emphasis">{{ currentGroup.name }}</div>
          </div>
        </div>

        <v-progress-linear v-if="isLoading" indeterminate color="primary" class="mb-4" />

        <!-- Mes souhaits -->
        <v-card class="mb-6" elevation="2">
          <v-card-title
            class="bg-primary pa-4 d-flex align-center justify-space-between"
            style="cursor: pointer;"
            @click="myWishesExpanded = !myWishesExpanded"
          >
            <div class="d-flex align-center">
              <v-icon class="mr-2" color="white">mdi-gift</v-icon>
              <span class="text-white">Mes souhaits ({{ myWishes.length }})</span>
            </div>
            <v-btn
              icon
              variant="text"
              size="small"
              @click.stop="myWishesExpanded = !myWishesExpanded"
            >
              <v-icon color="white">{{ myWishesExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
            </v-btn>
          </v-card-title>

          <v-expand-transition>
            <v-card-text v-show="myWishesExpanded" class="pa-0">
              <!-- Onglets pour filtrer -->
              <v-tabs v-model="tab" bg-color="grey-lighten-4">
                <v-tab value="reserved">
                  <v-icon class="mr-2">mdi-check-circle</v-icon>
                  Réservés ({{ myReservedWishes.length }})
                </v-tab>
                <v-tab value="unreserved">
                  <v-icon class="mr-2">mdi-circle-outline</v-icon>
                  Non réservés ({{ myUnreservedWishes.length }})
                </v-tab>
                <v-tab value="all">
                  <v-icon class="mr-2">mdi-gift-outline</v-icon>
                  Tous ({{ myWishes.length }})
                </v-tab>
              </v-tabs>

              <v-window v-model="tab">
                <!-- Souhaits réservés -->
              <v-window-item value="reserved">
                <div v-if="myReservedWishes.length === 0" class="text-center py-8">
                  <v-icon size="64" color="grey-lighten-1">mdi-gift-outline</v-icon>
                  <div class="text-h6 text-medium-emphasis mt-4">Aucun souhait réservé pour le moment</div>
                </div>
                <v-list v-else lines="three">
                  <v-list-item
                    v-for="wish in myReservedWishes"
                    :key="wish.id"
                    class="border-b"
                  >
                    <template v-slot:prepend>
                      <v-avatar v-if="wish.imageUrl" :image="wish.imageUrl" size="80" class="mr-4" />
                      <v-avatar v-else color="grey-lighten-2" size="80" class="mr-4">
                        <v-icon size="40" color="grey">mdi-gift</v-icon>
                      </v-avatar>
                    </template>

                    <v-list-item-title class="text-h6 mb-1">
                      {{ wish.giftName }}
                    </v-list-item-title>

                    <v-list-item-subtitle v-if="wish.description" class="mb-2">
                      {{ wish.description }}
                    </v-list-item-subtitle>

                    <v-list-item-subtitle v-if="wish.price" class="mb-2">
                      <v-chip size="small" color="success" variant="tonal">
                        <v-icon start size="small">mdi-currency-eur</v-icon>
                        {{ formatPrice(wish.price) }} €
                      </v-chip>
                    </v-list-item-subtitle>

                    <template v-slot:append>
                      <div class="d-flex align-center">
                        <UserAvatar
                          :avatar-id="getReserverAvatar(wish.reservedBy!)"
                          :size="40"
                          class="mr-2"
                        />
                        <div>
                          <div class="text-caption text-medium-emphasis">Réservé par</div>
                          <div class="font-weight-bold">{{ getReserverName(wish.reservedBy!) }}</div>
                        </div>
                      </div>
                    </template>
                  </v-list-item>
                </v-list>
              </v-window-item>

              <!-- Souhaits non réservés -->
              <v-window-item value="unreserved">
                <div v-if="myUnreservedWishes.length === 0" class="text-center py-8">
                  <v-icon size="64" color="success">mdi-check-all</v-icon>
                  <div class="text-h6 text-medium-emphasis mt-4">Tous vos souhaits sont réservés !</div>
                </div>
                <v-list v-else lines="three">
                  <v-list-item
                    v-for="wish in myUnreservedWishes"
                    :key="wish.id"
                    class="border-b"
                  >
                    <template v-slot:prepend>
                      <v-avatar v-if="wish.imageUrl" :image="wish.imageUrl" size="80" class="mr-4" />
                      <v-avatar v-else color="grey-lighten-2" size="80" class="mr-4">
                        <v-icon size="40" color="grey">mdi-gift</v-icon>
                      </v-avatar>
                    </template>

                    <v-list-item-title class="text-h6 mb-1">
                      {{ wish.giftName }}
                    </v-list-item-title>

                    <v-list-item-subtitle v-if="wish.description" class="mb-2">
                      {{ wish.description }}
                    </v-list-item-subtitle>

                    <v-list-item-subtitle v-if="wish.price" class="mb-2">
                      <v-chip size="small" color="success" variant="tonal">
                        <v-icon start size="small">mdi-currency-eur</v-icon>
                        {{ formatPrice(wish.price) }} €
                      </v-chip>
                    </v-list-item-subtitle>

                    <template v-slot:append>
                      <v-chip color="warning" variant="tonal">
                        <v-icon start size="small">mdi-clock-outline</v-icon>
                        En attente
                      </v-chip>
                    </template>
                  </v-list-item>
                </v-list>
              </v-window-item>

              <!-- Tous les souhaits -->
              <v-window-item value="all">
                <div v-if="myWishes.length === 0" class="text-center py-8">
                  <v-icon size="64" color="grey-lighten-1">mdi-gift-outline</v-icon>
                  <div class="text-h6 text-medium-emphasis mt-4">Vous n'avez pas encore de souhaits</div>
                </div>
                <v-list v-else lines="three">
                  <v-list-item
                    v-for="wish in myWishes"
                    :key="wish.id"
                    class="border-b"
                  >
                    <template v-slot:prepend>
                      <v-avatar v-if="wish.imageUrl" :image="wish.imageUrl" size="80" class="mr-4" />
                      <v-avatar v-else color="grey-lighten-2" size="80" class="mr-4">
                        <v-icon size="40" color="grey">mdi-gift</v-icon>
                      </v-avatar>
                    </template>

                    <v-list-item-title class="text-h6 mb-1">
                      {{ wish.giftName }}
                    </v-list-item-title>

                    <v-list-item-subtitle v-if="wish.description" class="mb-2">
                      {{ wish.description }}
                    </v-list-item-subtitle>

                    <v-list-item-subtitle v-if="wish.price" class="mb-2">
                      <v-chip size="small" color="success" variant="tonal">
                        <v-icon start size="small">mdi-currency-eur</v-icon>
                        {{ formatPrice(wish.price) }} €
                      </v-chip>
                    </v-list-item-subtitle>

                    <template v-slot:append>
                      <div v-if="wish.reservedBy" class="d-flex align-center">
                        <UserAvatar
                          :avatar-id="getReserverAvatar(wish.reservedBy)"
                          :size="40"
                          class="mr-2"
                        />
                        <div>
                          <div class="text-caption text-medium-emphasis">Réservé par</div>
                          <div class="font-weight-bold">{{ getReserverName(wish.reservedBy) }}</div>
                        </div>
                      </div>
                      <v-chip v-else color="warning" variant="tonal">
                        <v-icon start size="small">mdi-clock-outline</v-icon>
                        En attente
                      </v-chip>
                    </template>
                  </v-list-item>
                </v-list>
              </v-window-item>
            </v-window>
            </v-card-text>
          </v-expand-transition>
        </v-card>

        <!-- Vue synthétique des autres membres -->
        <v-card v-if="otherMembersSummary.length > 0" class="mb-6" elevation="2">
          <v-card-title class="bg-secondary pa-4">
            <v-icon class="mr-2" color="white">mdi-account-multiple</v-icon>
            <span class="text-white">Autres membres ({{ otherMembersSummary.length }})</span>
          </v-card-title>

          <v-card-text class="pa-4">
            <v-row>
              <v-col
                v-for="summary in otherMembersSummary"
                :key="summary.member.id"
                cols="12"
                md="6"
                lg="4"
              >
                <v-card
                  variant="outlined"
                  class="h-100 member-card"
                  hover
                  @click="goToMember(summary.member.id)"
                  style="cursor: pointer;"
                >
                  <v-card-text>
                    <!-- En-tête du membre -->
                    <div class="d-flex align-center mb-3">
                      <UserAvatar
                        :avatar-id="summary.member.avatarId"
                        :size="48"
                        class="mr-3"
                      />
                      <div class="flex-grow-1">
                        <div class="text-h6 font-weight-bold">
                          {{ authStore.getMemberDisplayName(summary.member) }}
                        </div>
                        <div class="text-caption text-medium-emphasis">
                          {{ summary.totalWishes }} souhait{{ summary.totalWishes > 1 ? 's' : '' }}
                        </div>
                      </div>
                    </div>

                    <!-- Statistiques -->
                    <div class="d-flex gap-2 mb-3">
                      <v-chip
                        size="small"
                        color="success"
                        variant="tonal"
                      >
                        <v-icon start size="small">mdi-check-circle</v-icon>
                        {{ summary.reservedCount }} réservé{{ summary.reservedCount > 1 ? 's' : '' }}
                      </v-chip>
                      <v-chip
                        v-if="summary.unreservedCount > 0"
                        size="small"
                        color="warning"
                        variant="tonal"
                      >
                        <v-icon start size="small">mdi-clock-outline</v-icon>
                        {{ summary.unreservedCount }} restant{{ summary.unreservedCount > 1 ? 's' : '' }}
                      </v-chip>
                    </div>

                    <!-- Détail des réservations -->
                    <v-divider class="mb-3" />

                    <div v-if="summary.reservationDetails.length > 0">
                      <div class="text-caption text-medium-emphasis mb-2 font-weight-bold">
                        Réservations :
                      </div>
                      <v-list density="compact" class="pa-0">
                        <v-list-item
                          v-for="detail in summary.reservationDetails"
                          :key="detail.wish.id"
                          class="px-0"
                          density="compact"
                        >
                          <template v-slot:prepend>
                            <v-icon size="small" color="success" class="mr-2">mdi-check</v-icon>
                          </template>

                          <v-list-item-title class="text-body-2">
                            {{ detail.wish.giftName }}
                          </v-list-item-title>

                          <template v-slot:append>
                            <div class="d-flex align-center">
                              <UserAvatar
                                :avatar-id="detail.reservedByAvatar"
                                :size="24"
                                class="mr-1"
                              />
                              <span class="text-caption">{{ detail.reservedByName }}</span>
                            </div>
                          </template>
                        </v-list-item>
                      </v-list>
                    </div>

                    <div v-else class="text-center py-3">
                      <v-icon color="grey-lighten-1">mdi-gift-off-outline</v-icon>
                      <div class="text-caption text-medium-emphasis">Aucun souhait réservé</div>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
export default {
  data() {
    return {
      tab: 'all'
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1600px;
  margin: 0 auto;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}

.member-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.member-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}
</style>

