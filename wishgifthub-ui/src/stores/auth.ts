import {computed, ref} from 'vue'
import {defineStore} from 'pinia'


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
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error('Accès réservé aux administrateurs')
        }
        throw new Error('Identifiants incorrects')
      }

      const data = await response.json()
      user.value = data.user
      token.value = data.token

      // Stocker le token dans localStorage
      localStorage.setItem('auth_token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erreur de connexion'
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

