import {Api} from '@/generated/api/wish/Api'
import {jwtDecode} from 'jwt-decode'
import router from '@/router'

/**
 * Instance singleton du client API
 */
let apiClient: Api | null = null


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
 * Configure l'intercepteur pour gérer les erreurs 401
 */
function setupInterceptor(api: Api) {
  // Intercepteur de réponse pour gérer les erreurs 401
  api.instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {

        // Token expiré ou invalide
        const storedToken = localStorage.getItem('auth_token')
        const currentPath = router.currentRoute.value.path

        let isAdmin = false

        if (storedToken) {
          try {
            const decodedToken = jwtDecode<JwtPayload>(storedToken)
            isAdmin = decodedToken.isAdmin
          } catch (decodeError) {
            isAdmin = true
          }
        }

        // Nettoyer le localStorage APRÈS avoir décodé le token
        localStorage.removeItem('auth_token')
        localStorage.removeItem('user')

        // Rediriger selon le rôle
        setTimeout(() => {
          if (isAdmin) {
            console.log('Redirection admin vers /')
            // Admin : rediriger vers la page d'accueil (login) avec un paramètre
            if (currentPath !== '/') {
              router.push({ path: '/', query: { expired: 'true' } })
            }
          } else {
            // User simple : rediriger vers la page d'expiration avec message
            if (currentPath !== '/token-expired') {
              router.push('/token-expired').finally(() => {
              })
            } else {
            }
          }
        }, 100) // Petit délai pour éviter les conflits de navigation
      }

      return Promise.reject(error)
    }
  )
}

/**
 * Initialise le client API avec le token d'authentification
 */
export function initApiClient(token?: string) {
  const baseURL =  '/'

  apiClient = new Api({
    baseURL,
    securityWorker: token
      ? async () => ({
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      : undefined,
  })

  // Configurer l'intercepteur
  setupInterceptor(apiClient)

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

