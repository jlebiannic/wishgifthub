import {Api} from '@/generated/api/wish/Api'

/**
 * Instance singleton du client API
 */
let apiClient: Api | null = null

/**
 * Initialise le client API avec le token d'authentification
 */
export function initApiClient(token?: string) {
  apiClient = new Api({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    securityWorker: token
      ? async () => ({
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      : undefined,
  })
  return apiClient
}

/**
 * Récupère l'instance du client API
 * Initialise une nouvelle instance si elle n'existe pas
 */
export function getApiClient(): Api {
  if (!apiClient) {
    apiClient = initApiClient()
  }
  return apiClient
}

/**
 * Met à jour le token d'authentification du client API
 */
export function updateApiToken(token: string | null) {
  if (token) {
    initApiClient(token)
  } else {
    apiClient = initApiClient()
  }
}

