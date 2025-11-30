import {ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient} from '@/api/client'
import type {GroupResponse, UserResponse} from '@/generated/api/wish/data-contracts'

/**
 * Interface représentant un groupe (mappée depuis GroupResponse)
 */
export interface Group {
  id: string
  name: string
  type: string
  adminId: string
  createdAt: string
}

/**
 * Interface représentant un membre du groupe
 */
export interface GroupMember {
  id: string
  email: string
  isAdmin?: boolean
  createdAt: string
}

/**
 * Store gérant les groupes et invitations
 */
export const useGroupStore = defineStore('group', () => {
  const groups = ref<Group[]>([])
  const members = ref<GroupMember[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Récupérer les groupes de l'utilisateur connecté
   */
  async function fetchMyGroups() {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUserGroups()

      // Mapper les GroupResponse vers notre interface Group
      groups.value = response.data.map((g: GroupResponse) => ({
        id: g.id,
        name: g.name,
        type: g.type,
        adminId: g.adminId,
        createdAt: g.createdAt
      }))
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des groupes'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupérer les membres d'un groupe
   */
  async function fetchGroupMembers(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.getUsersByGroup(groupId)

      // Mapper les UserResponse vers notre interface GroupMember
      members.value = response.data.map((u: UserResponse) => ({
        id: u.id,
        email: u.email,
        isAdmin: u.isAdmin,
        createdAt: u.createdAt
      }))
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la récupération des membres'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Créer un nouveau groupe
   */
  async function createGroup(name: string, type: 'noël' = 'noël') {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.createGroup({name, type})

      const newGroup: Group = {
        id: response.data.id,
        name: response.data.name,
        type: response.data.type,
        adminId: response.data.adminId,
        createdAt: response.data.createdAt
      }

      groups.value.push(newGroup)

      // Si un nouveau token JWT est retourné, le mettre à jour
      if (response.data.jwtToken) {
        // TODO: Mettre à jour le token dans le store auth
        console.log('Nouveau JWT reçu:', response.data.jwtToken)
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
   * Supprimer un groupe
   */
  async function deleteGroup(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      await apiClient.deleteGroup(groupId)

      // Retirer le groupe de la liste
      groups.value = groups.value.filter(g => g.id !== groupId)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la suppression du groupe'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Modifier un groupe
   */
  async function updateGroup(groupId: string, name: string, type: 'noël' = 'noël') {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.updateGroup(groupId, {name, type})

      // Mettre à jour le groupe dans la liste
      const index = groups.value.findIndex(g => g.id === groupId)
      if (index !== -1) {
        groups.value[index] = {
          id: response.data.id,
          name: response.data.name,
          type: response.data.type,
          adminId: response.data.adminId,
          createdAt: response.data.createdAt
        }
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la modification du groupe'
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Inviter un utilisateur dans un groupe
   */
  async function inviteUser(groupId: string, email: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.invite(groupId, {email})

      return {
        success: true,
        invitationLink: response.data.invitationLink || null
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de l\'invitation'
      return {
        success: false,
        invitationLink: null
      }
    } finally {
      isLoading.value = false
    }
  }

  return {
    groups,
    members,
    isLoading,
    error,
    fetchMyGroups,
    fetchGroupMembers,
    createGroup,
    deleteGroup,
    updateGroup,
    inviteUser,
  }
})

