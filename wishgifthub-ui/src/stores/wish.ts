import {ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient} from '@/api/client'
import type {WishRequest, WishResponse} from '@/generated/api/wish/data-contracts'

/**
 * Store gérant les souhaits (wishes) des utilisateurs
 */
export const useWishStore = defineStore('wish', () => {
  const wishes = ref<WishResponse[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Récupère tous les souhaits d'un groupe
   */
  async function fetchGroupWishes(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getGroupWishes(groupId)
      wishes.value = response.data
      return wishes.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des souhaits'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupère les souhaits d'un utilisateur spécifique
   */
  async function fetchUserWishes(groupId: string, userId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUserWishes(groupId, userId)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des souhaits'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupère mes souhaits
   */
  async function fetchMyWishes(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getMyWishes(groupId)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération de mes souhaits'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Ajoute un nouveau souhait
   */
  async function addWish(groupId: string, wish: WishRequest) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.addWish(groupId, wish)

      // Ajouter le souhait à la liste locale
      if (response.data) {
        wishes.value.push(response.data)
      }

      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de l\'ajout du souhait'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Supprime un souhait
   */
  async function deleteWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      await apiClient.deleteWish(groupId, wishId)

      // Retirer le souhait de la liste locale
      wishes.value = wishes.value.filter(w => w.id !== wishId)

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la suppression du souhait'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Réserve un souhait
   */
  async function reserveWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.reserveWish(groupId, wishId)

      // Mettre à jour le souhait dans la liste locale
      if (response.data) {
        const index = wishes.value.findIndex(w => w.id === wishId)
        if (index !== -1) {
          wishes.value[index] = response.data
        }
      }

      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la réservation du souhait'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Annule la réservation d'un souhait
   */
  async function unreserveWish(groupId: string, wishId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.unreserveWish(groupId, wishId)

      // Mettre à jour le souhait dans la liste locale
      if (response.data) {
        const index = wishes.value.findIndex(w => w.id === wishId)
        if (index !== -1) {
          wishes.value[index] = response.data
        }
      }

      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de l\'annulation de la réservation'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Réinitialise le store
   */
  function reset() {
    wishes.value = []
    error.value = null
    isLoading.value = false
  }

  return {
    wishes,
    isLoading,
    error,
    fetchGroupWishes,
    fetchUserWishes,
    fetchMyWishes,
    addWish,
    deleteWish,
    reserveWish,
    unreserveWish,
    reset,
  }
})

