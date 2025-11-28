import {ref} from 'vue'
import {defineStore} from 'pinia'
import {useAuthStore} from './auth'

const API_URL = import.meta.env.VITE_API_URL || ''

/**
 * Interface représentant un groupe
 */
export interface Group {
  id: string
  name: string
  description?: string
  createdBy: string
}

/**
 * Interface représentant une invitation
 */
export interface Invitation {
  id: string
  groupId: string
  groupName: string
  invitedUserId: string
  invitedUsername: string
  invitedUserEmail: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  createdAt: string
}

/**
 * Store gérant les groupes et invitations
 */
export const useGroupStore = defineStore('group', () => {
  const groups = ref<Group[]>([])
  const invitations = ref<Invitation[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Récupérer les groupes de l'utilisateur connecté
   */
  async function fetchMyGroups() {
    isLoading.value = true
    error.value = null

    try {
      const authStore = useAuthStore()
      const response = await fetch(`${API_URL}/api/users/my-groups`, {
        headers: {
          Authorization: `Bearer ${authStore.token}`,
        },
      })

      if (!response.ok) {
        throw new Error('Erreur lors de la récupération des groupes')
      }

      groups.value = await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erreur inconnue'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Récupérer les invitations d'un groupe
   */
  async function fetchGroupInvitations(groupId: string) {
    isLoading.value = true
    error.value = null

    try {
      const authStore = useAuthStore()
      const response = await fetch(`${API_URL}/api/groups/${groupId}/members`, {
        headers: {
          Authorization: `Bearer ${authStore.token}`,
        },
      })

      if (!response.ok) {
        throw new Error('Erreur lors de la récupération des invitations')
      }

      invitations.value = await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erreur inconnue'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Créer un nouveau groupe
   */
  async function createGroup(name: string, description?: string) {
    isLoading.value = true
    error.value = null

    try {
      const authStore = useAuthStore()
      const response = await fetch('/api/groups', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`,
        },
        body: JSON.stringify({ name, description }),
      })

      if (!response.ok) {
        throw new Error('Erreur lors de la création du groupe')
      }

      const newGroup = await response.json()
      groups.value.push(newGroup)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erreur inconnue'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    groups,
    invitations,
    isLoading,
    error,
    fetchMyGroups,
    fetchGroupInvitations,
    createGroup,
  }
})

