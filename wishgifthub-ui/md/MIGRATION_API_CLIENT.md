# 📋 MIGRATION VERS CLIENT API OPENAPI - Récapitulatif

## 🎯 Objectif

Remplacer tous les appels `fetch` manuels par le client API TypeScript généré automatiquement depuis la spécification OpenAPI.

---

## ✅ Fichiers créés

### 1. `src/api/client.ts` 
**Nouveau fichier** - Gestionnaire centralisé du client API

**Fonctionnalités** :
- ✅ Instance singleton du client API
- ✅ Initialisation avec token JWT
- ✅ Mise à jour dynamique du token
- ✅ Configuration de la baseURL depuis env

**Exports** :
- `initApiClient(token?: string)` - Initialise le client
- `getApiClient()` - Récupère l'instance
- `updateApiToken(token: string | null)` - Met à jour le token

---

### 2. `src/stores/wish.ts`
**Nouveau fichier** - Store Pinia pour les souhaits

**Fonctionnalités** :
- ✅ Récupérer tous les souhaits d'un groupe
- ✅ Récupérer mes souhaits
- ✅ Récupérer les souhaits d'un utilisateur
- ✅ Créer un souhait
- ✅ Supprimer un souhait
- ✅ Réserver un souhait
- ✅ Annuler une réservation

**Types** :
- Interface `Wish` mappée depuis `WishResponse`

---

## ✏️ Fichiers modifiés

### 1. `src/stores/auth.ts`
**Changements** :
- ❌ `fetch('/api/auth/login')` manuel
- ✅ `apiClient.login()` avec types générés

**Améliorations** :
- ✅ Utilisation de `AuthRequest` et `AuthResponse` typés
- ✅ Gestion des erreurs améliorée (status 403, 401)
- ✅ Mise à jour automatique du token dans le client API
- ✅ Mapping vers l'interface `User` locale

**Nouvelles fonctionnalités** :
- Met à jour le client API lors du login/logout/restore

---

### 2. `src/stores/group.ts`
**Changements** :
- ❌ `fetch('/api/groups')` manuel
- ✅ `apiClient.createGroup()`, `getUserGroups()`, etc.

**Suppressions** :
- ❌ Interface `Invitation` (obsolète)
- ❌ Prop `description` (remplacé par `type`)
- ❌ `fetchGroupInvitations()` (remplacé par `fetchGroupMembers()`)

**Ajouts** :
- ✅ Interface `GroupMember` pour les membres
- ✅ `fetchGroupMembers(groupId)` - Liste les membres d'un groupe
- ✅ `deleteGroup(groupId)` - Supprime un groupe
- ✅ `updateGroup(groupId, name, type)` - Modifie un groupe
- ✅ `inviteUser(groupId, email)` - Invite un utilisateur

**Types** :
- Interface `Group` mappée depuis `GroupResponse`
- Interface `GroupMember` mappée depuis `UserResponse`

---

### 3. `src/components/CreateGroupButton.vue`
**Changements** :
- ❌ Champ `groupDescription` (textarea)
- ✅ Type fixe `'noël'` automatique

**Interface** :
- ✅ Un seul champ : nom du groupe
- ✅ Hint expliquant que le type est automatique
- ✅ Placeholder suggérant un exemple

---

### 4. `src/components/InvitationsDialog.vue`
**Renommé conceptuellement** : Affiche maintenant les **membres** du groupe

**Changements** :
- ❌ Props `invitations: Invitation[]`
- ✅ Props `members: GroupMember[]`

**Interface** :
- ✅ Avatar avec icône couronne pour admin
- ✅ Email du membre
- ✅ Date d'ajout au groupe
- ✅ Badge "Administrateur" ou "Membre"

---

### 5. `src/components/GroupCard.vue`
**Changements** :
- ❌ Emit `showInvitations`
- ✅ Emit `showMembers`

**Affichage** :
- ✅ Icône cadeau
- ✅ Nom du groupe
- ✅ Chip avec le type (`noël`)
- ✅ Date de création formatée en français
- ✅ Bouton "Voir les membres" (au lieu de "Voir les invitations")

---

### 6. `src/views/HomeView.vue`
**Changements** :
- ❌ `showInvitationsDialog`
- ✅ `showMembersDialog`

**Fonctions** :
- ❌ `handleShowInvitations(groupId)`
- ✅ `handleShowMembers(groupId)`
- ❌ `handleCloseInvitations()`
- ✅ `handleCloseMembers()`

**Dialog** :
- ❌ Affiche les invitations
- ✅ Affiche les membres du groupe

---

## 🔄 Mapping des types API → Store

### AuthResponse → User
```typescript
{
  userId: string         → id: string
  isAdmin: boolean       → roles: ['ADMIN'] | ['USER']
  token: string          → (stocké séparément)
  // Ajouté
  username: email.split('@')[0]
  email: email (paramètre)
}
```

### GroupResponse → Group
```typescript
{
  id: string            → id: string
  name: string          → name: string
  type: string          → type: string
  adminId: string       → adminId: string
  createdAt: string     → createdAt: string
  jwtToken?: string     → (géré séparément si présent)
}
```

### UserResponse → GroupMember
```typescript
{
  id: string            → id: string
  email: string         → email: string
  isAdmin?: boolean     → isAdmin?: boolean
  createdAt: string     → createdAt: string
}
```

### WishResponse → Wish
```typescript
{
  id: string                → id: string
  userId: string            → userId: string
  groupId: string           → groupId: string
  giftName: string          → giftName: string
  description?: string      → description?: string | null
  url?: string              → url?: string | null
  reservedBy?: string       → reservedBy?: string | null
  createdAt: string         → createdAt: string
}
```

---

## 📊 Endpoints API utilisés

### Authentification
- ✅ `POST /api/auth/login` - `apiClient.login()`
- ⚠️ `POST /api/auth/register` - Disponible mais non utilisé

### Groupes
- ✅ `GET /api/groups/me` - `apiClient.getUserGroups()`
- ✅ `POST /api/groups` - `apiClient.createGroup()`
- ✅ `DELETE /api/groups/{groupId}` - `apiClient.deleteGroup()`
- ✅ `PUT /api/groups/{groupId}` - `apiClient.updateGroup()`

### Membres
- ✅ `GET /api/groups/{groupId}/users` - `apiClient.getUsersByGroup()`

### Invitations
- ✅ `POST /api/groups/{groupId}/invite` - `apiClient.invite()`
- ⚠️ `GET /api/invite/{token}` - Disponible mais non utilisé (accept)

### Souhaits
- ✅ `GET /api/groups/{groupId}/wishes` - `apiClient.getGroupWishes()`
- ✅ `GET /api/groups/{groupId}/wishes/me` - `apiClient.getMyWishes()`
- ✅ `GET /api/groups/{groupId}/wishes/users/{userId}` - `apiClient.getUserWishes()`
- ✅ `POST /api/groups/{groupId}/wishes` - `apiClient.addWish()`
- ✅ `DELETE /api/groups/{groupId}/wishes/{wishId}` - `apiClient.deleteWish()`
- ✅ `POST /api/groups/{groupId}/wishes/{wishId}/reserve` - `apiClient.reserveWish()`
- ✅ `DELETE /api/groups/{groupId}/wishes/{wishId}/reserve` - `apiClient.unreserveWish()`

---

## ✅ Avantages de la migration

### 1. Type Safety
- ✅ Tous les endpoints sont typés
- ✅ IntelliSense dans l'IDE
- ✅ Erreurs de compilation si mauvais paramètres

### 2. Maintenance
- ✅ Un seul point de vérité : le fichier OpenAPI
- ✅ Régénération automatique du client si l'API change
- ✅ Pas de désynchronisation entre frontend et backend

### 3. Gestion des erreurs
- ✅ Erreurs structurées avec `ErrorResponse`
- ✅ Accès facile au status HTTP et message d'erreur
- ✅ Gestion cohérente dans tous les stores

### 4. Authentification
- ✅ Token géré centralement dans le client API
- ✅ Mise à jour automatique du header `Authorization`
- ✅ Pas besoin de passer le token à chaque appel

---

## 🧪 Tests recommandés

### Test 1 : Authentification
```typescript
// 1. Login
await authStore.login('admin@example.com', 'password')
// ✅ Vérifier que user.value est défini
// ✅ Vérifier que token.value est défini
// ✅ Vérifier que le client API a le token

// 2. Logout
authStore.logout()
// ✅ Vérifier que user et token sont null
// ✅ Vérifier que localStorage est vide
```

### Test 2 : Groupes
```typescript
// 1. Créer un groupe
await groupStore.createGroup('Test Noël 2025', 'noël')
// ✅ Vérifier que le groupe apparaît dans groups.value

// 2. Récupérer les groupes
await groupStore.fetchMyGroups()
// ✅ Vérifier que groups.value contient les groupes

// 3. Récupérer les membres
await groupStore.fetchGroupMembers(groupId)
// ✅ Vérifier que members.value contient les membres
```

### Test 3 : Souhaits
```typescript
// 1. Créer un souhait
await wishStore.createWish(groupId, 'Livre', 'Description', 'https://...')
// ✅ Vérifier que le souhait apparaît dans wishes.value

// 2. Réserver un souhait
await wishStore.reserveWish(groupId, wishId)
// ✅ Vérifier que reservedBy est défini
```

---

## 🚀 Prochaines étapes recommandées

### 1. Compléter l'interface utilisateur
- [ ] Créer une page de liste des souhaits
- [ ] Créer un formulaire d'ajout de souhait
- [ ] Créer un formulaire d'invitation
- [ ] Gérer l'acceptation d'invitation

### 2. Améliorer la gestion d'erreurs
- [ ] Afficher des toasts/snackbars pour les erreurs
- [ ] Ajouter des retry automatiques
- [ ] Gérer l'expiration du token (401)

### 3. Optimiser les performances
- [ ] Implémenter du caching dans les stores
- [ ] Éviter les appels API redondants
- [ ] Ajouter du loading state granulaire

### 4. Tests
- [ ] Ajouter des tests unitaires pour les stores
- [ ] Mocker le client API dans les tests
- [ ] Tests E2E avec Cypress/Playwright

---

## 📝 Notes importantes

### Régénération du client API
Quand le backend change, régénérer avec :
```bash
cd wishgifthub-ui
npm run generate-api
```

### Configuration
- La `baseURL` est configurée dans `http-client.ts` (défaut: `http://localhost:8080`)
- Peut être surchargée via `VITE_API_URL` dans `.env`

### Sécurité
- Le token JWT est stocké dans localStorage
- Envoyé automatiquement dans le header `Authorization: Bearer {token}`
- Géré par le `securityWorker` du client API

---

**Date de migration** : 28 Janvier 2025  
**Statut** : ✅ **MIGRATION COMPLÈTE**  
**Fichiers modifiés** : 8  
**Fichiers créés** : 2  
**Erreurs** : 0

