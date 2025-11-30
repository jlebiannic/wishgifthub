import {ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient} from '@/api/client'
import type {WishResponse} from '@/generated/api/wish/data-contracts'

/**
 * Interface représentant un souhait (mappée depuis WishResponse)
 */
export interface Wish {
  id: string
  userId: string
  groupId: string
  giftName: string
  description?: string | null
  url?: string | null
  reservedBy?: string | null
  createdAt: string
}

/**
 * Store gérant les souhaits
 */
export const useWishStore = defineStore('wish', () => {
  const wishes = ref<Wish[]>([])
  const myWishes = ref<Wish[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Mapper une WishResponse vers notre interface Wish
   */
  function mapWishResponse(w: WishResponse): Wish {
    return {
      id: w.id,
      userId: w.userId,
      groupId: w.groupId,
      giftName: w.giftName,
      description: w.description,
      url: w.url,
      reservedBy: w.reservedBy,
      createdAt: w.createdAt
    }
  }

  /**
   * Récupérer tous les souhaits d'un groupe
   */
  async function fetchGroupWishes(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getGroupWishes(groupId)

      wishes.value = response.data.map(mapWishResponse)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des souhaits'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupérer mes souhaits dans un groupe
   */
  async function fetchMyWishes(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getMyWishes(groupId)

      myWishes.value = response.data.map(mapWishResponse)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération de vos souhaits'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupérer les souhaits d'un utilisateur spécifique
   */
  async function fetchUserWishes(groupId: string, userId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUserWishes(groupId, userId)

      return response.data.map(mapWishResponse)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des souhaits de l\'utilisateur'
      return []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Créer un nouveau souhait
   */
  async function createWish(
    groupId: string,
    giftName: string,
    description?: string,
    url?: string
  ) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.addWish(groupId, {
        giftName,
        description: description || null,
        url: url || null
      })

      const newWish = mapWishResponse(response.data)

      // Ajouter à la liste des souhaits
      wishes.value.push(newWish)
      myWishes.value.push(newWish)

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la création du souhait'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Supprimer un souhait
   */
  async function deleteWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      await apiClient.deleteWish(groupId, wishId)

      // Retirer des listes
      wishes.value = wishes.value.filter(w => w.id !== wishId)
      myWishes.value = myWishes.value.filter(w => w.id !== wishId)

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la suppression du souhait'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Réserver un souhait
   */
  async function reserveWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.reserveWish(groupId, wishId)

      const updatedWish = mapWishResponse(response.data)

      // Mettre à jour dans la liste
      const index = wishes.value.findIndex(w => w.id === wishId)
      if (index !== -1) {
        wishes.value[index] = updatedWish
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la réservation du souhait'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Annuler la réservation d'un souhait
   */
  async function unreserveWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.unreserveWish(groupId, wishId)

      const updatedWish = mapWishResponse(response.data)

      // Mettre à jour dans la liste
      const index = wishes.value.findIndex(w => w.id === wishId)
      if (index !== -1) {
        wishes.value[index] = updatedWish
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de l\'annulation de la réservation'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    wishes,
    myWishes,
    isLoading,
    error,
    fetchGroupWishes,
    fetchMyWishes,
    fetchUserWishes,
    createWish,
    deleteWish,
    reserveWish,
    unreserveWish,
  }
})

