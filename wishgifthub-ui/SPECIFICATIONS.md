# Spécifications de l'application WishGiftHub UI

## 1. Vue d'ensemble

Application Vue.js 3 + TypeScript + Vuetify 3 permettant la gestion collaborative de listes de souhaits organisées par groupes avec système d'invitations.

## 2. Architecture technique

### Stack technique
- **Framework**: Vue.js 3 avec Composition API
- **Langage**: TypeScript
- **UI Library**: Vuetify 3
- **State Management**: Pinia
- **Router**: Vue Router
- **HTTP Client**: Axios
- **Build Tool**: Vite
- **Tests**: Vitest + Vue Test Utils

### Configuration API
- **Base URL**: `http://localhost:8080/api` (configurable via env)
- **Authentication**: JWT Bearer Token
- **Content-Type**: `application/json`

## 3. Authentification et sécurité

### Gestion du JWT
```typescript
interface AuthToken {
  token: string
  expiresAt: number
}
```

- Stockage: `localStorage.setItem('authToken', token)`
- Header automatique: `Authorization: Bearer {token}`
- Décodage JWT pour extraire:
  - `sub`: userId
  - `email`: email de l'utilisateur
  - `isAdmin`: boolean (rôle administrateur)
  - `authorities`: liste des groupes `GROUP_{groupId}`

### Refresh du token
Après certaines opérations (création de groupe, acceptation d'invitation), le backend retourne un nouveau token avec les autorités mises à jour → remplacer le token dans le store.

## 4. Structure de routing

### Routes publiques (non authentifiées)

#### `GET /login`
**Description**: Page de connexion

**Composant**: `LoginView.vue`

**Fonctionnalités**:
- Formulaire avec email et password
- Validation:
  - Email: requis, format email valide
  - Password: requis, min 8 caractères
- Bouton "Se connecter"
- Lien vers `/register`
- **API**: `POST /api/auth/login`
  ```typescript
  {
    email: string
    password: string
  }
  → Response: { token: string }
  ```
- Après succès: stocker token + rediriger vers `/groups`

#### `GET /register`
**Description**: Page d'inscription administrateur

**Composant**: `RegisterView.vue`

**Fonctionnalités**:
- Formulaire avec email, password, confirmPassword
- Validation:
  - Email: requis, format email valide, unique (vérif serveur)
  - Password: requis, min 8 caractères
  - ConfirmPassword: doit correspondre
- Bouton "S'inscrire"
- Lien vers `/login`
- **API**: `POST /api/auth/register`
  ```typescript
  {
    email: string
    password: string
  }
  → Response: { token: string }
  ```
- Gestion erreur 409: "Cet email est déjà utilisé"
- Après succès: stocker token + rediriger vers `/groups`

#### `GET /invite/:token`
**Description**: Page d'acceptation d'invitation

**Composant**: `InviteView.vue`

**Fonctionnalités**:
- Affichage du nom du groupe
- Message: "Vous avez été invité à rejoindre le groupe {groupName}"
- Bouton "Accepter l'invitation"
- **API**: `POST /api/invite/{token}`
  ```typescript
  → Response: { 
    token: string // nouveau JWT avec autorité du groupe
    groupId: number
  }
  ```
- Gestion erreur 404: "Invitation invalide ou expirée"
- Après succès: stocker nouveau token + rediriger vers `/groups/{groupId}`

### Routes authentifiées

Toutes ces routes nécessitent un AuthGuard qui:
- Vérifie la présence du token
- Redirige vers `/login` si absent
- Intercepte les 401 pour déconnecter et rediriger

#### `GET /groups`
**Description**: Dashboard - Liste de tous les groupes de l'utilisateur

**Composant**: `GroupListView.vue`

**Layout**:
- Header: "Mes groupes"
- Bouton FAB "+" (admin uniquement) → `/groups/new`
- Grid de cartes `GroupCard`

**API**: `GET /api/groups/me`
```typescript
→ Response: GroupDTO[]

interface GroupDTO {
  id: number
  name: string
  type: 'FAMILY' | 'FRIENDS' | 'COUPLE' | 'COWORKERS' | 'OTHER'
  ownerId: number
  memberCount?: number
}
```

**GroupCard** affiche:
- Icône selon type (famille, amis, couple, collègues, autre)
- Nom du groupe
- Badge "Admin" si `ownerId === currentUserId`
- Nombre de membres (si disponible)
- Action: clic → `/groups/{id}`

#### `GET /groups/new` (AdminGuard)
**Description**: Formulaire de création de groupe

**Composant**: `GroupCreateView.vue`

**Formulaire**:
- Champ "Nom du groupe" (requis, 3-100 caractères)
- Select "Type" (requis):
  - FAMILY → "Famille"
  - FRIENDS → "Amis"
  - COUPLE → "Couple"
  - COWORKERS → "Collègues"
  - OTHER → "Autre"
- Bouton "Créer"
- Bouton "Annuler" → retour

**API**: `POST /api/groups`
```typescript
{
  name: string
  type: GroupType
}
→ Response: {
  group: GroupDTO
  token: string // nouveau JWT avec GROUP_{groupId}
}
```

**Après succès**:
- Stocker nouveau token
- Toast: "Groupe créé avec succès"
- Rediriger vers `/groups/{id}`

#### `GET /groups/:groupId` (GroupMemberGuard)
**Description**: Vue détaillée d'un groupe

**Composant**: `GroupDetailView.vue`

**GroupMemberGuard**: vérifie que l'utilisateur a l'autorité `GROUP_{groupId}` ou est admin

**Layout**:
- Header: Nom du groupe + type (badge)
- Tabs Vuetify:
  1. **Membres**
  2. **Tous les souhaits**
  3. **Mes souhaits**
  4. **Paramètres** (admin propriétaire uniquement)

---

##### Tab 1: Membres

**API**: `GET /api/groups/{groupId}/users`
```typescript
→ Response: UserDTO[]

interface UserDTO {
  id: number
  email: string
}
```

**Affichage**:
- Liste avec avatar + email
- Badge "Propriétaire" sur le owner
- Bouton "Inviter un membre" (admin) → ouvre `InvitationDialog`

**InvitationDialog**:
- Champ email
- **API**: `POST /api/groups/{groupId}/invite`
  ```typescript
  {
    email: string
  }
  → Response: { token: string } // token d'invitation
  ```
- Affiche le lien: `{frontendUrl}/invite/{token}`
- Bouton "Copier le lien"
- Toast: "Invitation créée ! Lien copié"

---

##### Tab 2: Tous les souhaits

**API**: `GET /api/groups/{groupId}/wishes`
```typescript
→ Response: WishDTO[]

interface WishDTO {
  id: number
  giftName: string
  description?: string
  url?: string
  ownerId: number
  ownerEmail: string
  reservedBy?: number
  reservedByEmail?: string
  groupId: number
}
```

**Affichage**:
- Filtre par utilisateur (select)
- Grid de cartes `WishCard`

**WishCard** affiche:
- Nom du cadeau (titre)
- Description (si présente)
- Lien (bouton "Voir le lien" si présent)
- Propriétaire: "Souhait de {ownerEmail}"
- Badge de statut:
  - Si `reservedBy === null`: Badge vert "Disponible"
  - Si `reservedBy === currentUserId`: Badge orange "Réservé par vous"
  - Sinon: Badge rouge "Réservé" (sans nom du réserveur)

**Actions**:
- Si propriétaire du souhait: masquer le bouton de réservation
- Si `reservedBy === null`:
  - Bouton "Réserver"
  - **API**: `POST /api/groups/{groupId}/wishes/{wishId}/reserve`
- Si `reservedBy === currentUserId`:
  - Bouton "Annuler la réservation"
  - **API**: `DELETE /api/groups/{groupId}/wishes/{wishId}/reserve`
- Si réservé par quelqu'un d'autre: pas d'action possible

---

##### Tab 3: Mes souhaits

**API**: `GET /api/groups/{groupId}/wishes/me`
```typescript
→ Response: WishDTO[]
```

**Affichage**:
- Bouton FAB "+" → ouvre `WishFormDialog` en mode création
- Liste de cartes `MyWishCard`

**MyWishCard** affiche:
- Nom du cadeau
- Description
- Lien (avec icône)
- Badge de statut (réservé ou disponible, sans nom)
- Actions:
  - Bouton "Modifier" → ouvre `WishFormDialog` en mode édition
  - Bouton "Supprimer" → confirmation + appel API

**WishFormDialog** (mode création):
- Champ "Nom du cadeau" (requis, 3-100 caractères)
- Textarea "Description" (optionnel, max 500 caractères)
- Champ "Lien URL" (optionnel, format URL valide)
- **API**: `POST /api/groups/{groupId}/wishes`
  ```typescript
  {
    giftName: string
    description?: string
    url?: string
  }
  → Response: WishDTO
  ```

**WishFormDialog** (mode édition):
- Mêmes champs pré-remplis
- **Note**: L'API ne fournit pas d'endpoint PUT/PATCH pour modifier
- **Solution**: Supprimer + recréer OU demander l'ajout de l'endpoint

**Suppression**:
- Confirmation: "Êtes-vous sûr de vouloir supprimer ce souhait ?"
- **API**: `DELETE /api/groups/{groupId}/wishes/{wishId}`
- Toast: "Souhait supprimé"

---

##### Tab 4: Paramètres (OwnerGuard)

**OwnerGuard**: vérifie que `group.ownerId === currentUserId`

**Section 1: Informations du groupe**
- Formulaire éditable avec nom et type
- **API**: `PUT /api/groups/{groupId}`
  ```typescript
  {
    name: string
    type: GroupType
  }
  → Response: GroupDTO
  ```
- Bouton "Enregistrer les modifications"

**Section 2: Zone de danger**
- Bouton rouge "Supprimer le groupe"
- Confirmation avec saisie du nom du groupe
- **API**: `DELETE /api/groups/{groupId}`
- Après succès:
  - Toast: "Groupe supprimé"
  - Rediriger vers `/groups`
  - **Important**: Le token actuel contient encore `GROUP_{groupId}` → demander un refresh du token OU rediriger vers logout/login

---

#### `GET /groups/:groupId/users/:userId/wishes` (GroupMemberGuard)
**Description**: Souhaits d'un utilisateur spécifique

**Composant**: `UserWishesView.vue`

**API**: `GET /api/groups/{groupId}/wishes/users/{userId}`
```typescript
→ Response: WishDTO[]
```

**Affichage**:
- Header: "Souhaits de {userEmail}"
- Grid de cartes `WishCard` (même composant que Tab 2)
- Possibilité de réserver/annuler

**Navigation**: accessible depuis le clic sur un utilisateur dans l'onglet Membres

## 5. Stores Pinia

### authStore

```typescript
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('authToken') as string | null,
    user: null as DecodedUser | null,
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.user?.isAdmin ?? false,
    userId: (state) => state.user?.sub,
    userEmail: (state) => state.user?.email,
    groupIds: (state) => state.user?.authorities
      ?.filter(a => a.startsWith('GROUP_'))
      .map(a => parseInt(a.replace('GROUP_', ''))) ?? [],
  },
  
  actions: {
    async login(credentials: { email: string; password: string }) {
      const response = await axios.post('/api/auth/login', credentials)
      this.setToken(response.data.token)
    },
    
    async register(credentials: { email: string; password: string }) {
      const response = await axios.post('/api/auth/register', credentials)
      this.setToken(response.data.token)
    },
    
    async acceptInvitation(token: string) {
      const response = await axios.post(`/api/invite/${token}`)
      this.setToken(response.data.token)
      return response.data.groupId
    },
    
    setToken(token: string) {
      this.token = token
      localStorage.setItem('authToken', token)
      this.user = this.decodeToken(token)
      this.setupAxiosInterceptor()
    },
    
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('authToken')
      router.push('/login')
    },
    
    decodeToken(token: string): DecodedUser {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return {
        sub: payload.sub,
        email: payload.email,
        isAdmin: payload.isAdmin,
        authorities: payload.authorities,
        exp: payload.exp,
      }
    },
    
    setupAxiosInterceptor() {
      axios.interceptors.request.use(config => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`
        }
        return config
      })
      
      axios.interceptors.response.use(
        response => response,
        error => {
          if (error.response?.status === 401) {
            this.logout()
          }
          return Promise.reject(error)
        }
      )
    },
  },
})

interface DecodedUser {
  sub: number
  email: string
  isAdmin: boolean
  authorities: string[]
  exp: number
}
```

### groupStore

```typescript
export const useGroupStore = defineStore('group', {
  state: () => ({
    groups: [] as GroupDTO[],
    currentGroup: null as GroupDTO | null,
    members: [] as UserDTO[],
  }),
  
  actions: {
    async fetchMyGroups() {
      const response = await axios.get('/api/groups/me')
      this.groups = response.data
    },
    
    async fetchGroup(id: number) {
      // Note: pas d'endpoint GET /api/groups/{id} dans l'API
      // On utilise fetchMyGroups et on filtre
      await this.fetchMyGroups()
      this.currentGroup = this.groups.find(g => g.id === id) ?? null
    },
    
    async createGroup(data: { name: string; type: string }) {
      const response = await axios.post('/api/groups', data)
      const authStore = useAuthStore()
      authStore.setToken(response.data.token) // refresh JWT
      this.currentGroup = response.data.group
      return response.data.group
    },
    
    async updateGroup(id: number, data: { name: string; type: string }) {
      const response = await axios.put(`/api/groups/${id}`, data)
      this.currentGroup = response.data
      await this.fetchMyGroups() // refresh liste
    },
    
    async deleteGroup(id: number) {
      await axios.delete(`/api/groups/${id}`)
      this.groups = this.groups.filter(g => g.id !== id)
      this.currentGroup = null
    },
    
    async fetchMembers(groupId: number) {
      const response = await axios.get(`/api/groups/${groupId}/users`)
      this.members = response.data
    },
    
    async inviteMember(groupId: number, email: string) {
      const response = await axios.post(`/api/groups/${groupId}/invite`, { email })
      return response.data.token // token d'invitation
    },
  },
})
```

### wishStore

```typescript
export const useWishStore = defineStore('wish', {
  state: () => ({
    groupWishes: [] as WishDTO[],
    myWishes: [] as WishDTO[],
    userWishes: [] as WishDTO[],
  }),
  
  actions: {
    async fetchGroupWishes(groupId: number) {
      const response = await axios.get(`/api/groups/${groupId}/wishes`)
      this.groupWishes = response.data
    },
    
    async fetchMyWishes(groupId: number) {
      const response = await axios.get(`/api/groups/${groupId}/wishes/me`)
      this.myWishes = response.data
    },
    
    async fetchUserWishes(groupId: number, userId: number) {
      const response = await axios.get(`/api/groups/${groupId}/wishes/users/${userId}`)
      this.userWishes = response.data
    },
    
    async createWish(groupId: number, data: CreateWishDTO) {
      const response = await axios.post(`/api/groups/${groupId}/wishes`, data)
      this.myWishes.push(response.data)
      return response.data
    },
    
    async deleteWish(groupId: number, wishId: number) {
      await axios.delete(`/api/groups/${groupId}/wishes/${wishId}`)
      this.myWishes = this.myWishes.filter(w => w.id !== wishId)
      this.groupWishes = this.groupWishes.filter(w => w.id !== wishId)
    },
    
    async reserveWish(groupId: number, wishId: number) {
      const response = await axios.post(`/api/groups/${groupId}/wishes/${wishId}/reserve`)
      this.updateWishInLists(response.data)
    },
    
    async unreserveWish(groupId: number, wishId: number) {
      const response = await axios.delete(`/api/groups/${groupId}/wishes/${wishId}/reserve`)
      this.updateWishInLists(response.data)
    },
    
    updateWishInLists(updatedWish: WishDTO) {
      const updateList = (list: WishDTO[]) => {
        const index = list.findIndex(w => w.id === updatedWish.id)
        if (index !== -1) list[index] = updatedWish
      }
      updateList(this.groupWishes)
      updateList(this.myWishes)
      updateList(this.userWishes)
    },
  },
})
```

## 6. Composants réutilisables

### NavigationBar.vue
**Props**: aucune

**Affichage**:
- Logo + titre "WishGiftHub"
- Menu navigation:
  - "Mes groupes" → `/groups`
- Menu utilisateur (avatar + email):
  - Se déconnecter → `authStore.logout()`

### GroupCard.vue
**Props**:
```typescript
{
  group: GroupDTO
  isOwner: boolean
}
```

**Affichage**:
- `v-card` avec:
  - Icône selon type (mdi-account-group, mdi-account-multiple, mdi-heart, mdi-briefcase, mdi-dots-horizontal)
  - Titre: nom du groupe
  - Badge "Admin" si `isOwner`
  - Sous-titre: type traduit + nombre de membres
  - Action: `@click` → navigation

### WishCard.vue
**Props**:
```typescript
{
  wish: WishDTO
  canReserve: boolean
  isOwner: boolean
}
```

**Affichage**:
- `v-card` avec:
  - Titre: giftName
  - Description (si présente)
  - Badge statut (couleur selon réservation)
  - Bouton lien (si URL)
  - Bouton "Réserver" / "Annuler" si `canReserve`

**Events**:
- `@reserve` → émis au clic sur réserver
- `@unreserve` → émis au clic sur annuler

### WishFormDialog.vue
**Props**:
```typescript
{
  modelValue: boolean
  wish?: WishDTO // si mode édition
}
```

**Affichage**:
- `v-dialog` avec formulaire
- Validation avec règles Vuetify
- Boutons: Annuler, Enregistrer

**Events**:
- `@update:modelValue` → fermeture
- `@save` → émis avec les données du formulaire

### InvitationDialog.vue
**Props**:
```typescript
{
  modelValue: boolean
  groupId: number
}
```

**Affichage**:
- `v-dialog` avec:
  - Champ email
  - Zone de résultat (lien généré)
  - Bouton "Copier le lien"

**Events**:
- `@update:modelValue` → fermeture
- `@invited` → émis après création

## 7. Gestion des erreurs

### Intercepteur Axios global

```typescript
axios.interceptors.response.use(
  response => response,
  error => {
    const { status, data } = error.response || {}
    
    const snackbarStore = useSnackbarStore()
    
    switch (status) {
      case 400:
        snackbarStore.showError(data?.message || 'Données invalides')
        break
      case 401:
        snackbarStore.showError('Session expirée')
        useAuthStore().logout()
        break
      case 403:
        snackbarStore.showError('Accès refusé')
        router.push('/groups')
        break
      case 404:
        snackbarStore.showError('Ressource introuvable')
        break
      case 409:
        snackbarStore.showError('Cet email est déjà utilisé')
        break
      default:
        snackbarStore.showError('Une erreur est survenue')
    }
    
    return Promise.reject(error)
  }
)
```

### SnackbarStore (notifications)

```typescript
export const useSnackbarStore = defineStore('snackbar', {
  state: () => ({
    visible: false,
    message: '',
    color: 'info',
  }),
  
  actions: {
    showSuccess(message: string) {
      this.show(message, 'success')
    },
    
    showError(message: string) {
      this.show(message, 'error')
    },
    
    showInfo(message: string) {
      this.show(message, 'info')
    },
    
    show(message: string, color: string) {
      this.message = message
      this.color = color
      this.visible = true
    },
  },
})
```

## 8. Navigation Guards

### router/index.ts

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/invite/:token',
      name: 'Invite',
      component: () => import('@/views/InviteView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/groups',
      name: 'Groups',
      component: () => import('@/views/GroupListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/groups/new',
      name: 'GroupCreate',
      component: () => import('@/views/GroupCreateView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/groups/:groupId',
      name: 'GroupDetail',
      component: () => import('@/views/GroupDetailView.vue'),
      meta: { requiresAuth: true, requiresGroupMember: true },
    },
    {
      path: '/groups/:groupId/users/:userId/wishes',
      name: 'UserWishes',
      component: () => import('@/views/UserWishesView.vue'),
      meta: { requiresAuth: true, requiresGroupMember: true },
    },
    {
      path: '/',
      redirect: '/groups',
    },
  ],
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Rediriger les utilisateurs connectés loin des pages publiques
  if (!to.meta.requiresAuth && authStore.isAuthenticated) {
    return next('/groups')
  }
  
  // Vérifier l'authentification
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next('/login')
  }
  
  // Vérifier le rôle admin
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next('/groups')
  }
  
  // Vérifier l'appartenance au groupe
  if (to.meta.requiresGroupMember) {
    const groupId = parseInt(to.params.groupId as string)
    if (!authStore.groupIds.includes(groupId)) {
      return next('/groups')
    }
  }
  
  next()
})

export default router
```

## 9. Types TypeScript

### models/Group.ts

```typescript
export interface GroupDTO {
  id: number
  name: string
  type: GroupType
  ownerId: number
  memberCount?: number
}

export type GroupType = 'FAMILY' | 'FRIENDS' | 'COUPLE' | 'COWORKERS' | 'OTHER'

export const GROUP_TYPE_LABELS: Record<GroupType, string> = {
  FAMILY: 'Famille',
  FRIENDS: 'Amis',
  COUPLE: 'Couple',
  COWORKERS: 'Collègues',
  OTHER: 'Autre',
}

export const GROUP_TYPE_ICONS: Record<GroupType, string> = {
  FAMILY: 'mdi-account-group',
  FRIENDS: 'mdi-account-multiple',
  COUPLE: 'mdi-heart',
  COWORKERS: 'mdi-briefcase',
  OTHER: 'mdi-dots-horizontal',
}

export interface CreateGroupDTO {
  name: string
  type: GroupType
}
```

### models/Wish.ts

```typescript
export interface WishDTO {
  id: number
  giftName: string
  description?: string
  url?: string
  ownerId: number
  ownerEmail: string
  reservedBy?: number
  reservedByEmail?: string
  groupId: number
}

export interface CreateWishDTO {
  giftName: string
  description?: string
  url?: string
}
```

### models/User.ts

```typescript
export interface UserDTO {
  id: number
  email: string
}

export interface RegisterDTO {
  email: string
  password: string
}

export interface LoginDTO {
  email: string
  password: string
}
```

### models/Auth.ts

```typescript
export interface AuthResponseDTO {
  token: string
}

export interface InviteResponseDTO {
  token: string
  groupId?: number
}
```

## 10. Fonctionnalités UX avancées

### Loading states
- Utiliser `v-progress-circular` ou `v-skeleton-loader` pendant les appels API
- Store `loadingStore` pour gérer l'état global de chargement

### Confirmations
- Utiliser `v-dialog` avec `ConfirmDialog.vue` réutilisable
- Actions destructives: suppression de groupe, souhait

### Copie en un clic
- Utiliser l'API Clipboard: `navigator.clipboard.writeText(text)`
- Toast de confirmation

### Dark mode
- Utiliser Vuetify theme toggle
- Persister dans localStorage

### Responsive
- Mobile-first avec breakpoints Vuetify
- Navigation drawer pour mobile
- Bottom navigation pour mobile

### Animations
- Transitions Vue:
  - `<transition name="fade">` pour les dialogs
  - `<transition-group>` pour les listes
- Ripple effect Vuetify sur les boutons

### Internationalisation (future)
- Préparer vue-i18n
- Fichiers de traduction fr/en

## 11. Variables d'environnement

### .env.development
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=WishGiftHub
```

### .env.production
```
VITE_API_BASE_URL=https://api.wishgifthub.com/api
VITE_APP_NAME=WishGiftHub
```

## 12. Améliorations futures

### Fonctionnalités manquantes dans l'API
1. **GET /api/groups/{id}**: récupérer un groupe par ID
2. **PATCH /api/groups/{id}/wishes/{wishId}**: modifier un souhait
3. **GET /api/groups/{id}/invitations**: lister les invitations en attente
4. **DELETE /api/groups/{id}/users/{userId}**: retirer un membre

### Fonctionnalités UI futures
- Recherche de souhaits
- Tri et filtres avancés
- Notifications en temps réel (WebSocket)
- Mode hors ligne (PWA)
- Upload d'images pour les souhaits
- Commentaires sur les souhaits
- Historique des réservations
- Export PDF de la liste

## 13. Tests

### Tests unitaires (Vitest)
- Stores Pinia
- Composants avec Vue Test Utils
- Couverture > 80%

### Tests E2E (Playwright)
- Parcours utilisateur complet:
  - Inscription → Création groupe → Invitation → Ajout souhait → Réservation

## 14. Performance

### Optimisations
- Lazy loading des routes
- Debounce sur les recherches
- Cache des requêtes GET (vue-query ou tanstack-query)
- Compression des images
- Code splitting par route

### Métriques
- First Contentful Paint < 1.5s
- Time to Interactive < 3s
- Lighthouse score > 90

---

**Date de dernière mise à jour**: 2025-11-17
**Version de l'API**: openapi.yaml

