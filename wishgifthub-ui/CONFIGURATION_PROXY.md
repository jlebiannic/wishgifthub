# 🔄 Configuration Proxy - WishGiftHub UI

## ✅ Problème Résolu : CORS

Au lieu d'utiliser une configuration CORS côté backend, l'application utilise maintenant un **proxy Vite** pour rediriger les requêtes API.

## 📋 Configuration du Proxy

### Dans `vite.config.ts`

```typescript
export default defineConfig({
  // ...
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### Comment ça fonctionne ?

1. **Frontend** : `http://localhost:3000`
2. **Requête** : `fetch('/api/auth/login')`
3. **Proxy Vite** : Redirige vers `http://localhost:8080/api/auth/login`
4. **Backend** : Traite la requête
5. **Réponse** : Retourne au frontend

## 🔧 Changements Effectués

### Fichiers Modifiés

1. **`src/stores/auth.ts`**
   - ❌ Supprimé : `const API_URL = import.meta.env.VITE_API_URL || ''`
   - ✅ Ajouté : URLs relatives `'/api/auth/login'`

2. **`src/stores/group.ts`**
   - ❌ Supprimé : `const API_URL = import.meta.env.VITE_API_URL || ''`
   - ✅ Ajouté : URLs relatives pour tous les endpoints

### Fichiers Supprimés

- ❌ `.env` - Plus nécessaire avec le proxy

### Fichiers Mis à Jour

- ✅ `.env.example` - Documentation mise à jour

## 🚀 Utilisation

### Développement Local

```bash
# 1. Démarrer le backend Spring Boot
cd wishgifthub-api
mvn spring-boot:run
# Backend sur http://localhost:8080

# 2. Démarrer le frontend Vue
cd wishgifthub-ui
npm run dev
# Frontend sur http://localhost:3000
# Proxy automatique vers http://localhost:8080
```

### Tester la Configuration

```bash
# Dans le navigateur, ouvrir la console
# Faire une requête de connexion

# La requête sera :
# FROM: http://localhost:3000/api/auth/login
# TO:   http://localhost:8080/api/auth/login (via proxy)
```

## ⚙️ Changer l'URL du Backend

### Pour le Développement

Éditer `vite.config.ts` :

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://votre-backend:PORT',  // ← Modifier ici
      changeOrigin: true
    }
  }
}
```

### Pour la Production

En production, vous aurez plusieurs options :

#### Option 1 : Backend et Frontend sur le même domaine
```
https://monapp.com/          → Frontend (Vue)
https://monapp.com/api/      → Backend (Spring Boot)
```
Pas de proxy nécessaire, configuration Nginx/Apache.

#### Option 2 : Domaines différents
```
https://app.monapp.com       → Frontend
https://api.monapp.com       → Backend
```
Utiliser une variable d'environnement :

```typescript
// vite.config.ts
const apiUrl = process.env.VITE_API_URL || 'http://localhost:8080'

export default defineConfig({
  // ...
  server: {
    proxy: {
      '/api': {
        target: apiUrl,
        changeOrigin: true
      }
    }
  }
})
```

Puis créer `.env.production` :
```
VITE_API_URL=https://api.monapp.com
```

## 🔍 Avantages du Proxy

### ✅ Avantages

1. **Pas de CORS** - Même origine pour le navigateur
2. **Simplicité** - Configuration côté frontend uniquement
3. **Développement facile** - Pas de configuration backend
4. **Sécurité** - Pas d'exposition de l'URL backend au client
5. **Flexibilité** - Changement facile de l'URL backend

### ⚠️ Limitations

1. **Développement uniquement** - Le proxy Vite ne fonctionne qu'en dev
2. **Production** - Nécessite une configuration serveur (Nginx, etc.)
3. **WebSockets** - Configuration supplémentaire si nécessaire

## 🐛 Dépannage

### Erreur : "fetch failed" ou "ECONNREFUSED"

**Problème** : Le backend n'est pas démarré

**Solution** :
```bash
cd wishgifthub-api
mvn spring-boot:run
```

### Erreur : "404 Not Found" sur /api/*

**Problème** : Le proxy n'est pas configuré

**Solution** : Vérifier `vite.config.ts` et redémarrer le serveur dev
```bash
npm run dev
```

### Erreur : Les requêtes sont lentes

**Problème** : `changeOrigin: true` manquant

**Solution** : Vérifier la config proxy :
```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true  // ← Important !
  }
}
```

### Erreur : CORS même avec le proxy

**Problème** : Utilisation d'URL absolue au lieu de relative

**Solution** : Vérifier les stores
```typescript
// ❌ Mauvais
fetch('http://localhost:8080/api/auth/login')

// ✅ Bon
fetch('/api/auth/login')
```

## 📊 Comparaison : Proxy vs CORS

| Critère | Proxy Vite | CORS Backend |
|---------|------------|--------------|
| Configuration | Frontend uniquement | Backend uniquement |
| Développement | ✅ Très simple | ⚠️ Configuration Spring |
| Production | ⚠️ Besoin Nginx/Apache | ✅ Fonctionne directement |
| Sécurité | ✅ URL backend cachée | ⚠️ URL exposée |
| Performance | ✅ Pas de preflight | ⚠️ Requête OPTIONS |
| WebSockets | ⚠️ Config supplémentaire | ✅ Fonctionne |

## 📝 Conclusion

La configuration proxy est **idéale pour le développement** car :
- ✅ Pas de configuration backend nécessaire
- ✅ Pas de problèmes CORS
- ✅ URLs relatives simples
- ✅ Flexibilité de configuration

Pour la production, vous devrez configurer un serveur web (Nginx, Apache) pour servir le frontend et proxifier vers le backend.

---

**Date** : 18 novembre 2025  
**Configuration** : ✅ Proxy Vite activé  
**Backend** : http://localhost:8080  
**Frontend** : http://localhost:3000  
**Statut** : ✅ Prêt pour le développement

