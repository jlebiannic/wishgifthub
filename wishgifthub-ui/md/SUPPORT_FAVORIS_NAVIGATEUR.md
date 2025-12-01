# ✅ Support des favoris navigateur et rafraîchissement de page

## 🎯 Fonctionnalité implémentée

L'application supporte maintenant **les favoris du navigateur** et **le rafraîchissement de page**. Vous pouvez :

- ✅ Mettre en favoris n'importe quelle page de l'application
- ✅ Rafraîchir la page (F5) sans perdre votre état
- ✅ Fermer le navigateur et rouvrir un favori plus tard
- ✅ Partager des liens directs vers des groupes

---

## 🔧 Implémentation

### 1. URLs persistantes

Chaque page a une URL unique qui contient toutes les informations nécessaires :

#### Page d'accueil
```
http://localhost:3000/
```

#### Page d'un groupe (membres + souhaits)
```
http://localhost:3000/group/123e4567-e89b-12d3-a456-426614174000
```

L'UUID du groupe est dans l'URL → **Peut être mis en favori** ✅

#### Page d'acceptation d'invitation
```
http://localhost:3000/invite/abc12345-def6-7890-ghij-klmnopqrstuv
```

Le token d'invitation est dans l'URL → **Lien partageable** ✅

---

## 🔄 Restauration de session automatique

### Fichier modifié : `GroupMembersView.vue`

```typescript
onMounted(async () => {
  if (!groupId.value) {
    router.push('/')
    return
  }

  // ✨ Restaurer la session si elle existe (pour les favoris/rafraîchissement)
  if (!authStore.isAuthenticated) {
    await authStore.restoreSession()
  }

  // Si toujours pas authentifié après restauration, rediriger vers accueil
  if (!authStore.isAuthenticated) {
    router.push('/')
    return
  }

  try {
    // Charger les données du groupe
    // ...
  } catch (error) {
    // En cas d'erreur, retourner à l'accueil
    router.push('/')
  }
})
```

### Fonctionnement

1. **L'utilisateur ouvre un favori** ou rafraîchit la page
2. **Vue Router** charge la page avec l'URL
3. **onMounted** s'exécute :
   - Lit le `groupId` depuis l'URL (`route.params.groupId`)
   - Vérifie si l'utilisateur est authentifié
   - Si non → **Restaure la session depuis localStorage**
   - Charge le token JWT
   - Récupère les données du groupe
4. **La page s'affiche** exactement comme avant

---

## 📦 Données persistantes

### localStorage

Les données suivantes sont sauvegardées dans le navigateur :

```javascript
localStorage.setItem('auth_token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...')
localStorage.setItem('user', '{"id":"...","email":"...","roles":[...]}')
```

**Ces données permettent** :
- ✅ Restauration automatique de la session
- ✅ Persistance entre les rechargements de page
- ✅ Fonctionnement des favoris

**Durée de vie** :
- Jusqu'à expiration du token JWT (par défaut : 24h)
- Ou jusqu'à déconnexion manuelle

---

## 🎯 Scénarios d'utilisation

### Scénario 1 : Mettre un groupe en favori

1. **Se connecter** à l'application
2. **Cliquer sur un groupe** → URL devient `/group/{groupId}`
3. **Ajouter aux favoris** (Ctrl+D)
   ```
   Titre : Noël en famille 2025
   URL : http://localhost:3000/group/123e4567...
   ```
4. **Fermer le navigateur**
5. **Rouvrir le favori plus tard**
   - ✅ La session est restaurée automatiquement
   - ✅ Le groupe s'affiche directement
   - ✅ Les membres et souhaits sont chargés

### Scénario 2 : Rafraîchir la page

1. **Être sur la page d'un groupe**
2. **Appuyer sur F5** (rafraîchir)
3. ✅ La page se recharge
4. ✅ La session est restaurée
5. ✅ Le groupe s'affiche à nouveau

### Scénario 3 : Partager un lien

1. **Admin** crée une invitation
2. **Copie le lien d'invitation**
   ```
   http://localhost:3000/invite/abc12345-def6...
   ```
3. **Envoie le lien** par email/SMS
4. **Destinataire** clique sur le lien
5. ✅ Page d'acceptation s'ouvre
6. ✅ Invitation acceptée automatiquement
7. ✅ Utilisateur connecté et redirigé

### Scénario 4 : Session expirée

1. **Ouvrir un favori** après plusieurs jours
2. **Token JWT expiré**
3. ✅ Redirection automatique vers l'accueil
4. ✅ Message : "Veuillez vous reconnecter"
5. L'utilisateur se reconnecte
6. Peut retourner au groupe manuellement

---

## 🔐 Sécurité et gestion d'erreurs

### Protection des routes

```typescript
// Si pas de groupId dans l'URL
if (!groupId.value) {
  router.push('/')
  return
}

// Si pas authentifié après restauration de session
if (!authStore.isAuthenticated) {
  router.push('/')
  return
}

// Si erreur lors du chargement (ex: groupe inexistant ou non autorisé)
catch (error) {
  router.push('/')
}
```

### Cas gérés

| Situation | Comportement |
|-----------|--------------|
| **Token valide** | ✅ Session restaurée, page affichée |
| **Token expiré** | ⚠️ Redirection vers accueil, demande de reconnexion |
| **Pas de token** | ⚠️ Redirection vers accueil (page de login) |
| **Groupe inexistant** | ⚠️ Redirection vers accueil après erreur |
| **Pas accès au groupe** | ⚠️ Erreur 403, redirection vers accueil |

---

## 🌐 URLs de l'application

### Routes publiques (sans authentification)

```
/                          → Page d'accueil (login si non connecté)
/invite/{token}            → Acceptation d'invitation (connexion auto)
```

### Routes protégées (authentification requise)

```
/group/{groupId}           → Page du groupe (membres + souhaits)
```

**Toutes ces URLs** :
- ✅ Peuvent être mises en favoris
- ✅ Peuvent être rafraîchies (F5)
- ✅ Restaurent l'état correct de la page

---

## 💾 Stockage des données

### localStorage (persistant)

```javascript
{
  "auth_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": "{\"id\":\"...\",\"email\":\"user@example.com\",\"roles\":[\"USER\"],\"groupIds\":[...]}"
}
```

### sessionStorage (non utilisé actuellement)

Pourrait être utilisé pour :
- État des cartes (étendues/repliées)
- Filtres de recherche
- Position de scroll

### Cookies (non utilisés)

L'application n'utilise pas de cookies, tout est dans le JWT.

---

## 🎨 Expérience utilisateur

### Avant (sans support des favoris)

❌ Clic sur favori → Page blanche ou erreur  
❌ Rafraîchir (F5) → Perte de session  
❌ Fermer/rouvrir navigateur → Doit se reconnecter  

### Après (avec support)

✅ Clic sur favori → Page du groupe s'affiche directement  
✅ Rafraîchir (F5) → Page se recharge correctement  
✅ Fermer/rouvrir navigateur → Session restaurée automatiquement  

---

## 🔄 Diagramme de flux

### Ouverture d'un favori

```
User clique sur favori
         ↓
Vue Router charge /group/{groupId}
         ↓
GroupMembersView.onMounted()
         ↓
Vérifie si authentifié?
    Non → restoreSession()
    Oui → Continue
         ↓
Token valide?
    Non → Redirect vers /
    Oui → Continue
         ↓
Charge les données:
  - Informations du groupe
  - Liste des membres
  - Liste des souhaits
         ↓
Affiche la page ✅
```

---

## 🧪 Tests

### Test 1 : Favori basique

1. Se connecter
2. Aller dans un groupe
3. Ajouter aux favoris (Ctrl+D)
4. Fermer le navigateur
5. Rouvrir le favori
6. ✅ La page du groupe s'affiche directement

### Test 2 : Rafraîchissement

1. Se connecter
2. Aller dans un groupe
3. Appuyer sur F5
4. ✅ La page se recharge avec les mêmes données

### Test 3 : Token expiré

1. Se connecter
2. Créer un favori
3. Attendre expiration du token (24h)
4. Ouvrir le favori
5. ✅ Redirection vers page de connexion

### Test 4 : Groupe supprimé

1. Créer un favori d'un groupe
2. Supprimer le groupe (en base)
3. Ouvrir le favori
4. ✅ Redirection vers accueil (erreur gérée)

---

## 📝 Notes techniques

### Vue Router

```typescript
const route = useRoute()
const groupId = ref(route.params.groupId as string)
```

L'ID du groupe est **extrait de l'URL** au montage du composant.

### Auth Store

```typescript
async function restoreSession() {
  const storedToken = localStorage.getItem('auth_token')
  const storedUser = localStorage.getItem('user')
  
  if (storedToken && storedUser) {
    token.value = storedToken
    user.value = JSON.parse(storedUser)
    updateApiToken(storedToken)
    
    // Récupérer les groupes automatiquement
    if (groupIds.length > 0) {
      await groupStore.fetchMyGroups()
    }
  }
}
```

La session est **restaurée automatiquement** depuis le localStorage.

---

## ✅ Avantages

### Pour l'utilisateur

- 🚀 **Accès rapide** : Favoris → Direct au groupe
- 💾 **Persistance** : Pas besoin de se reconnecter constamment
- 🔗 **Partage** : Peut partager des liens directs
- 🔄 **Rafraîchissement** : F5 fonctionne normalement

### Pour l'application

- ✅ **SEO-friendly** : URLs propres et significatives
- ✅ **Bookmarkable** : Toutes les pages peuvent être sauvegardées
- ✅ **Shareable** : Les liens fonctionnent pour tout le monde
- ✅ **Robuste** : Gestion d'erreur complète

---

## 🚀 Fonctionnalités futures

### Améliorations possibles

1. **Query params pour l'état des cartes**
   ```
   /group/{id}?expanded=user1,user2
   ```
   Restaure quelles cartes étaient étendues

2. **Scroll position**
   Restaure la position de scroll dans la page

3. **Filtres et recherche**
   ```
   /group/{id}?search=macbook&filter=reserved
   ```

4. **Deep linking**
   ```
   /group/{id}/member/{userId}/wish/{wishId}
   ```
   Lien direct vers un souhait spécifique

---

## ✅ Conclusion

L'application supporte maintenant **complètement** :

- ✅ Les favoris du navigateur
- ✅ Le rafraîchissement de page (F5)
- ✅ La fermeture/réouverture du navigateur
- ✅ Le partage de liens directs
- ✅ La restauration automatique de session

**Les utilisateurs peuvent sauvegarder leurs pages préférées et y revenir facilement !** 🎉

