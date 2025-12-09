import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import {jwtDecode} from 'jwt-decode'
import {getApiClient, updateApiToken} from '@/api/client'
import {useGroupStore} from './group'
import type {UserResponse} from '@/generated/api/wish/data-contracts'

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
  pseudo?: string | null
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

      console.log('Auth data received:', authData) // Debug log

      // Construire l'objet User à partir de la réponse
      user.value = {
        id: authData.userId,
        username: email.split('@')[0] || email,
        email: email,
        roles: authData.isAdmin ? ['ADMIN'] : ['USER'],
        groupIds: groupIds,
        avatarId: authData.avatarId,
        pseudo: authData.pseudo
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
  async function loginWithToken(jwtToken: string, email: string, avatarId?: string | null, pseudo?: string | null) {
    isLoading.value = true
    error.value = null

    try {
      // Décoder le token JWT pour extraire les informations
      const decodedToken = jwtDecode<JwtPayload>(jwtToken)
      const groupIds = decodedToken.groupIds || []
      const userId = decodedToken.sub

      // Construire l'objet User avec les informations reçues de l'invitation
      user.value = {
        id: userId,
        username: email.split('@')[0] || email,
        email: email,
        roles: decodedToken.isAdmin ? ['ADMIN'] : ['USER'],
        groupIds: groupIds,
        avatarId: avatarId || null,
        pseudo: pseudo || null
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
        // Décoder le token pour vérifier son expiration
        try {
          const decodedToken = jwtDecode<JwtPayload>(storedToken)

          // Vérifier si le token est expiré
          const currentTime = Math.floor(Date.now() / 1000)
          if (decodedToken.exp && decodedToken.exp < currentTime) {
            // Token expiré - ne pas restaurer la session
            console.log('Token expiré lors de la restauration de session')
            logout()
            return
          }

          token.value = storedToken
          user.value = JSON.parse(storedUser)
          // Mettre à jour le client API avec le token restauré
          updateApiToken(storedToken)

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
          // Le token est peut-être invalide, nettoyer le localStorage
          logout()
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
   * Met à jour l'avatar et/ou le pseudo de l'utilisateur
   */
  async function updateAvatar(avatarId: string | null, pseudo: string | null) {
    try {
      const apiClient = getApiClient()
      const updateData: any = {}

      // Ne mettre à jour que les champs fournis (non null et différents de la valeur actuelle)
      if (avatarId !== null && avatarId !== user.value?.avatarId) {
        updateData.avatarId = avatarId
      }
      if (pseudo !== null && pseudo !== user.value?.pseudo) {
        updateData.pseudo = pseudo
      }

      // Si aucun changement, ne pas appeler l'API
      if (Object.keys(updateData).length === 0) {
        return true
      }

      const response = await apiClient.updateUserAvatar(updateData)

      // Mettre à jour l'avatarId et le pseudo dans l'objet user
      if (user.value) {
        user.value.avatarId = response.data.avatarId
        user.value.pseudo = response.data.pseudo
        localStorage.setItem('user', JSON.stringify(user.value))

        // Mettre à jour le profil dans le store group si le membre est dans la liste
        const groupStore = useGroupStore()
        groupStore.updateMemberProfile(user.value.id, {
          avatarId: response.data.avatarId,
          pseudo: response.data.pseudo
        })
      }

      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Erreur lors de la mise à jour du profil'
      return false
    }
  }

  /**
   * Retourne le nom d'affichage d'un membre selon la priorité suivante :
   * 1. Pseudo (si défini)
   * 2. Username extrait de l'email (partie avant le @)
   * 3. Email complet (fallback)
   *
   * @param member - L'objet utilisateur (UserResponse ou User)
   * @returns Le nom à afficher (string)
   */
  function getMemberDisplayName(member: UserResponse | User | null | undefined): string {
    if (!member) return ''

    // 1. Priorité au pseudo s'il existe
    if (member.pseudo) {
      return member.pseudo
    }

    // 2. Sinon, extraire le username de l'email (partie avant le @)
    const emailParts = member.email.split('@')
    const username = emailParts[0]

    // 3. Fallback sur l'email complet si pas de username
    return username || member.email
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
    getMemberDisplayName,
  }
})
