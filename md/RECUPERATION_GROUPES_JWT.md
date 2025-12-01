# Récupération automatique des groupes depuis le JWT

## Fonctionnalité implémentée

Lorsqu'un administrateur se connecte, l'application récupère automatiquement les groupes dont il est membre en utilisant les IDs contenus dans le token JWT.

## Modifications apportées

### 1. Store Auth (`src/stores/auth.ts`)

- **Ajout de l'interface `JwtPayload`** : Définit la structure du token JWT décodé
  ```typescript
  interface JwtPayload {
    sub: string // userId
    isAdmin: boolean
    groupIds?: string[]
    iat: number
    exp: number
  }
  ```

- **Ajout du champ `groupIds`** dans l'interface `User`

- **Modification de la fonction `login`** :
  - Décode le token JWT pour extraire les `groupIds`
  - Stocke les `groupIds` dans l'objet `User`
  - Récupère automatiquement les détails des groupes via le `groupStore` si l'utilisateur est admin

- **Modification de la fonction `restoreSession`** :
  - Décode le token JWT lors de la restauration de session
  - Récupère les groupes automatiquement

- **Modification de la fonction `logout`** :
  - Réinitialise le `groupStore` lors de la déconnexion

### 2. Store Group (nouveau fichier : `src/stores/group.ts`)

Nouveau store Pinia pour gérer les groupes et leurs membres :

- **Types exportés** :
  - `Group` : Alias de `GroupResponse`
  - `GroupMember` : Alias de `UserResponse`

- **États** :
  - `groups` : Liste des groupes
  - `members` : Liste des membres d'un groupe
  - `isLoading` : Indicateur de chargement
  - `error` : Message d'erreur

- **Actions** :
  - `fetchGroups()` : Récupère tous les groupes de l'admin
  - `fetchMyGroups()` : Récupère les groupes de l'utilisateur connecté (admin ou invité)
  - `fetchGroupsByIds(groupIds)` : Récupère les groupes par leurs IDs (utilisé en interne)
  - `fetchGroupMembers(groupId)` : Récupère les membres d'un groupe
  - `createGroup(name, type)` : Crée un nouveau groupe
  - `reset()` : Réinitialise le store

### 3. Dépendance ajoutée

Installation de la bibliothèque `jwt-decode` pour décoder les tokens JWT côté client :

```bash
npm install jwt-decode
```

## Flux d'authentification

1. L'utilisateur se connecte avec son email et mot de passe
2. Le backend génère un token JWT contenant :
   - L'ID utilisateur (`sub`)
   - Le flag admin (`isAdmin`)
   - Les IDs des groupes (`groupIds`)
3. Le frontend reçoit le token et l'AuthResponse
4. Le store auth décode le token JWT pour extraire les `groupIds`
5. Si l'utilisateur est admin et a des groupes, le store récupère automatiquement les détails des groupes via l'API
6. Les groupes sont affichés dans l'interface utilisateur

## Backend

Le backend génère déjà le token avec les `groupIds` dans le service `AuthService.loginAdmin()` :

```java
// Récupération de tous les groupes de l'utilisateur
List<UUID> groupIds = userGroupRepository.findByUserId(user.getId())
    .stream()
    .map(userGroup -> userGroup.getGroup().getId())
    .collect(Collectors.toList());

// Génération du token avec les groupIds
String token = jwtService.generateToken(user, groupIds);
```

## Avantages

- ✅ **Automatique** : Pas besoin de faire un appel API supplémentaire pour récupérer les groupes après le login
- ✅ **Sécurisé** : Les groupIds sont vérifiés côté backend et signés dans le JWT
- ✅ **Optimisé** : Utilise les informations déjà présentes dans le token
- ✅ **Persistant** : Lors de la restauration de session, les groupes sont automatiquement rechargés

