import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import {jwtDecode} from 'jwt-decode'
import {getApiClient, updateApiToken} from '@/api/client'
import {useGroupStore} from './group'

/**
 * Interface représentant un utilisateur authentifié
 */
export interface User {
  id: string
  username: string
  email: string
  roles: string[]
  groupIds?: string[]
  avatarId?: string | null
}

/**
 * Interface pour le payload du JWT
 */
interface JwtPayload {
  sub: string // userId
  isAdmin: boolean
  groupIds?: string[]
  iat: number
  exp: number
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

      // Décoder le token JWT pour extraire les groupIds
      const decodedToken = jwtDecode<JwtPayload>(authData.token)
      const groupIds = decodedToken.groupIds || []

      // Construire l'objet User à partir de la réponse
      user.value = {
        id: authData.userId,
        username: email.split('@')[0] || email,
        email: email,
        roles: authData.isAdmin ? ['ADMIN'] : ['USER'],
        groupIds: groupIds,
        avatarId: authData.avatarId
      }
      token.value = authData.token

      // Mettre à jour le client API avec le nouveau token
      updateApiToken(authData.token)

      // Stocker le token dans localStorage
      localStorage.setItem('auth_token', authData.token)
      localStorage.setItem('user', JSON.stringify(user.value))

      // Récupérer les groupes de l'utilisateur
      if (authData.isAdmin && groupIds.length > 0) {
        const groupStore = useGroupStore()
        await groupStore.fetchGroups()
      }

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
   * Connexion automatique avec un token JWT (par exemple après acceptation d'invitation)
   */
  async function loginWithToken(jwtToken: string, email: string) {
    isLoading.value = true
    error.value = null

    try {
      // Décoder le token JWT pour extraire les informations
      const decodedToken = jwtDecode<JwtPayload>(jwtToken)
      const groupIds = decodedToken.groupIds || []
      const userId = decodedToken.sub

      // Construire l'objet User
      user.value = {
        id: userId,
        username: email.split('@')[0] || email,
        email: email,
        roles: decodedToken.isAdmin ? ['ADMIN'] : ['USER'],
        groupIds: groupIds
      }
      token.value = jwtToken

      // Mettre à jour le client API avec le nouveau token
      updateApiToken(jwtToken)

      // Stocker le token dans localStorage
      localStorage.setItem('auth_token', jwtToken)
      localStorage.setItem('user', JSON.stringify(user.value))

      // Récupérer les groupes de l'utilisateur
      if (groupIds.length > 0) {
        const groupStore = useGroupStore()
        await groupStore.fetchMyGroups()
      }

      return true
    } catch (err: any) {
      error.value = err.message || 'Erreur lors de la connexion automatique'
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
    // Réinitialiser le store group
    const groupStore = useGroupStore()
    groupStore.reset()
  }

  /**
   * Restaurer la session depuis localStorage
   */
  async function restoreSession() {
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

        // Décoder le token pour extraire les groupIds et les récupérer
        try {
          const decodedToken = jwtDecode<JwtPayload>(storedToken)
          const groupIds = decodedToken.groupIds || []

          // Récupérer les groupes pour tous les utilisateurs (admin ou non)
          if (groupIds.length > 0) {
            const groupStore = useGroupStore()
            if (decodedToken.isAdmin) {
              await groupStore.fetchGroups()
            } else {
              await groupStore.fetchMyGroups()
            }
          }
        } catch (decodeError) {
          console.error('Erreur lors du décodage du token:', decodeError)
          // Le token est peut-être expiré ou invalide, on continue sans récupérer les groupes
        }
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

  /**
   * Met à jour le token JWT (par exemple après création d'un groupe)
   */
  function updateToken(newToken: string) {
    token.value = newToken

    // Mettre à jour le localStorage
    localStorage.setItem('auth_token', newToken)

    // Mettre à jour le client API avec le nouveau token
    updateApiToken(newToken)

    // Décoder le nouveau token pour mettre à jour les groupIds de l'utilisateur
    try {
      const decodedToken = jwtDecode<JwtPayload>(newToken)
      if (user.value) {
        user.value.groupIds = decodedToken.groupIds || []
        localStorage.setItem('user', JSON.stringify(user.value))
      }
    } catch (err) {
      console.error('Erreur lors du décodage du nouveau token:', err)
    }
  }

  /**
   * Met à jour l'avatar de l'utilisateur
   */
  async function updateAvatar(avatarId: string | null) {
    try {
      const apiClient = getApiClient()
      const response = await apiClient.updateUserAvatar({ avatarId })

      // Mettre à jour l'avatarId dans l'objet user
      if (user.value) {
        user.value.avatarId = response.data.avatarId
        localStorage.setItem('user', JSON.stringify(user.value))
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la mise à jour de l\'avatar'
      return false
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
    loginWithToken,
    logout,
    restoreSession,
    updateToken,
    updateAvatar,
  }
})
