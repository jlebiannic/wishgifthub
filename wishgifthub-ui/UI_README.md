# WishGiftHub UI

Interface utilisateur Vue.js pour l'application WishGiftHub.

## 🚀 Démarrage rapide

### Prérequis
- Node.js >= 20.19.0 ou >= 22.12.0
- npm

### Installation

```bash
cd wishgifthub-ui
npm install
```

### Développement

Démarrer le serveur de développement sur http://localhost:3000

```bash
npm run dev
```

L'application sera accessible à l'adresse : **http://localhost:3000**

### Build de production

```bash
npm run build
```

Les fichiers de production seront générés dans le dossier `dist/`.

### Prévisualiser la version de production

```bash
npm run preview
```

## 🛠️ Technologies utilisées

- **Vue 3** - Framework JavaScript progressif
- **TypeScript** - Typage statique
- **Vite** - Build tool et serveur de développement
- **Vue Router** - Routage côté client
- **Pinia** - State management
- **Vitest** - Framework de test unitaire
- **ESLint** - Linter JavaScript/TypeScript
- **Prettier** - Formateur de code

## 📁 Structure du projet

```
wishgifthub-ui/
├── public/              # Fichiers statiques
├── src/
│   ├── assets/         # Images, styles, etc.
│   ├── components/     # Composants Vue réutilisables
│   │   └── HelloWish.vue
│   ├── router/         # Configuration des routes
│   ├── stores/         # Stores Pinia
│   ├── views/          # Composants de pages
│   ├── App.vue         # Composant racine
│   └── main.ts         # Point d'entrée de l'application
├── index.html          # Template HTML
├── vite.config.ts      # Configuration Vite
├── tsconfig.json       # Configuration TypeScript
└── package.json        # Dépendances npm
```

## 🔧 Configuration

### Proxy API

Le serveur de développement Vite est configuré pour proxyfier les requêtes vers l'API backend :

- **Frontend** : http://localhost:3000
- **API Backend** : http://localhost:8080
- **Proxy** : `/api/*` → `http://localhost:8080/api/*`

### Scripts disponibles

- `npm run dev` - Démarrer le serveur de développement
- `npm run build` - Build de production
- `npm run preview` - Prévisualiser le build de production
- `npm run test:unit` - Lancer les tests unitaires
- `npm run type-check` - Vérifier les types TypeScript
- `npm run lint` - Linter et corriger le code
- `npm run format` - Formater le code avec Prettier

## 📝 Composant principal

Le composant `HelloWish.vue` affiche un message de bienvenue stylisé avec :
- Une icône de cadeau animée 🎁
- Le message "Hello Wish !"
- Un sous-titre de bienvenue
- Un design moderne avec gradient et animations

## 🔗 Intégration avec l'API

Pour communiquer avec l'API backend, vous pouvez utiliser `fetch` ou `axios` :

```typescript
// Exemple avec fetch
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ email, password })
})
```

Les requêtes vers `/api/*` seront automatiquement proxifiées vers `http://localhost:8080/api/*` en développement.

## 📚 Documentation

- [Vue 3 Documentation](https://vuejs.org/)
- [Vite Documentation](https://vitejs.dev/)
- [Vue Router Documentation](https://router.vuejs.org/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [TypeScript Documentation](https://www.typescriptlang.org/)

## 🎨 Personnalisation

Le style global de l'application peut être modifié dans :
- `src/assets/main.css` - Styles globaux
- `src/assets/base.css` - Variables CSS et reset

Pour personnaliser un composant, utilisez les balises `<style scoped>` dans les fichiers `.vue`.

