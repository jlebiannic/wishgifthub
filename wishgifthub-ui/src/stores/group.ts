import {ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient} from '@/api/client'
import type {GroupResponse, UserResponse} from '@/generated/api/wish/data-contracts'

/**
 * Type pour un groupe (alias de GroupResponse)
 */
export type Group = GroupResponse

/**
 * Type pour un membre de groupe (alias de UserResponse)
 */
export type GroupMember = UserResponse

/**
 * Store gérant les groupes de l'utilisateur
 */
export const useGroupStore = defineStore('group', () => {
  const groups = ref<GroupResponse[]>([])
  const members = ref<UserResponse[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Récupère tous les groupes de l'administrateur
   */
  async function fetchGroups() {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getGroups()
      groups.value = response.data
      return groups.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des groupes'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupère les groupes de l'utilisateur connecté (admin ou invité)
   */
  async function fetchMyGroups() {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUserGroups()
      groups.value = response.data
      return groups.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des groupes'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupère les groupes par leurs IDs
   */
  async function fetchGroupsByIds(groupIds: string[]) {
    if (groupIds.length === 0) {
      groups.value = []
      return groups.value
    }

    // Pour l'instant, on récupère tous les groupes et on filtre
    // Une optimisation future pourrait être d'ajouter un endpoint pour récupérer par IDs
    await fetchGroups()
    groups.value = groups.value.filter(group => groupIds.includes(group.id))
    return groups.value
  }

  /**
   * Récupère les membres d'un groupe
   */
  async function fetchGroupMembers(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUsersByGroup(groupId)
      members.value = response.data
      return members.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des membres'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Crée un nouveau groupe
   */
  async function createGroup(name: string, type: 'noël' = 'noël') {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.createGroup({name, type})

      // Ajouter le nouveau groupe à la liste
      if (response.data) {
        groups.value.push(response.data)
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la création du groupe'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Réinitialise le store
   */
  function reset() {
    groups.value = []
    members.value = []
    error.value = null
    isLoading.value = false
  }

  return {
    groups,
    members,
    isLoading,
    error,
    fetchGroups,
    fetchMyGroups,
    fetchGroupsByIds,
    fetchGroupMembers,
    createGroup,
    reset,
  }
})

