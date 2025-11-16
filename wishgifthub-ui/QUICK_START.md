# 🚀 Démarrage rapide - WishGiftHub UI

## Lancer l'interface Vue.js

### Option 1 : Commande simple

```bash
cd wishgifthub-ui
npm run dev
```

### Option 2 : Étape par étape

```bash
# 1. Aller dans le dossier du module UI
cd C:\Users\jlebiannic\dev\Poc\WishGiftHub\wishgifthub\wishgifthub-ui

# 2. Lancer le serveur de développement
npm run dev
```

## 📍 Accès à l'application

Une fois le serveur démarré, ouvrez votre navigateur à :

```
http://localhost:3000
```

Vous devriez voir :
- 🎁 Une icône cadeau animée
- **"Hello Wish !"** en grand titre
- Un message de bienvenue

## 🛠️ Environnement complet

Pour tester l'application complète avec l'API backend :

### Terminal 1 - API Backend (Spring Boot)
```bash
cd wishgifthub-api
mvn spring-boot:run
```
→ API disponible sur http://localhost:8080

### Terminal 2 - Frontend (Vue.js)
```bash
cd wishgifthub-ui
npm run dev
```
→ Interface disponible sur http://localhost:3000

## 📝 Vérification

Si tout fonctionne correctement :
- ✅ Le navigateur affiche "Hello Wish !"
- ✅ Pas d'erreurs dans la console
- ✅ Le Hot Module Replacement (HMR) fonctionne (modifications en temps réel)

## ❗ En cas de problème

### Erreur "Cannot find module"
```bash
cd wishgifthub-ui
npm install
```

### Port 3000 déjà utilisé
Modifiez le port dans `vite.config.ts` :
```typescript
server: {
  port: 3001, // Changez ici
  // ...
}
```

### Erreurs TypeScript
```bash
npm run type-check
```

## 📚 Documentation

- README complet : `wishgifthub-ui/UI_README.md`
- Récapitulatif : `VUEJS_MODULE_CREATED.md`

---

**🎁 Profitez de votre nouvelle interface Vue.js !**

