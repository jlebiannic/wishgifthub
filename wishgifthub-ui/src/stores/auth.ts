import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient, updateApiToken} from '@/api/client'

/**
 * Interface représentant un utilisateur authentifié
 */
export interface User {
  id: string
  username: string
  email: string
  roles: string[]
}

/**
 * Store gérant l'authentification et l'état de l'utilisateur
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)

  /**
   * Connexion de l'utilisateur
   */
  async function login(email: string, password: string) {
    isLoading.value = true
    error.value = null

    try {
      const apiClient = getApiClient()
      const response = await apiClient.login({email, password})

      const authData = response.data

      // Construire l'objet User à partir de la réponse
      user.value = {
        id: authData.userId,
        username: email.split('@')[0] || email, // Utiliser la partie avant @ comme username
        email: email,
        roles: authData.isAdmin ? ['ADMIN'] : ['USER']
      }
      token.value = authData.token

      // Mettre à jour le client API avec le nouveau token
      updateApiToken(authData.token)

      // Stocker le token dans localStorage
      localStorage.setItem('auth_token', authData.token)
      localStorage.setItem('user', JSON.stringify(user.value))

      return true
    } catch (err: any) {
      if (err.response?.status === 403) {
        error.value = 'Accès réservé aux administrateurs'
      } else if (err.response?.status === 401) {
        error.value = 'Identifiants incorrects'
      } else {
        error.value = err.response?.data?.message || 'Erreur de connexion'
      }
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Déconnexion de l'utilisateur
   */
  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user')
    // Réinitialiser le client API
    updateApiToken(null)
  }

  /**
   * Restaurer la session depuis localStorage
   */
  function restoreSession() {
    try {
      const storedToken = localStorage.getItem('auth_token')
      const storedUser = localStorage.getItem('user')

      // Vérifier que les valeurs existent et ne sont pas "undefined" ou "null"
      if (
        storedToken &&
        storedUser &&
        storedToken !== 'undefined' &&
        storedToken !== 'null' &&
        storedUser !== 'undefined' &&
        storedUser !== 'null'
      ) {
        token.value = storedToken
        user.value = JSON.parse(storedUser)
        // Mettre à jour le client API avec le token restauré
        updateApiToken(storedToken)
      } else {
        // Nettoyer le localStorage si les données sont invalides
        logout()
      }
    } catch (err) {
      // En cas d'erreur de parsing JSON, nettoyer le localStorage
      console.error('Erreur lors de la restauration de la session:', err)
      logout()
    }
  }

  return {
    user,
    token,
    isLoading,
    error,
    isAuthenticated,
    isAdmin,
    login,
    logout,
    restoreSession,
  }
})

