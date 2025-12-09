# Système de Versionnement

## Description

L'application génère automatiquement des informations de version lors de chaque build, incluant :
- La version de l'application (depuis package.json)
- La date et l'heure exacte du déploiement (année, mois, jour, heure, minute, seconde)
- Un timestamp ISO pour référence technique

## Fichiers générés

Lors du build, deux fichiers sont automatiquement générés :

1. **`public/version.json`** - Fichier JSON accessible via HTTP
2. **`src/version.ts`** - Fichier TypeScript pour import dans le code

Ces fichiers sont ignorés par Git (voir `.gitignore`).

## Affichage de la version

### Dans le navigateur

1. **Bouton flottant en bas à droite** : Un bouton d'information (icône `i`) est affiché en permanence dans le coin inférieur droit de l'application
2. **Cliquer sur le bouton** : Affiche une boîte de dialogue avec :
   - La version de l'application
   - La date de déploiement (format lisible : `YYYY-MM-DD HH:MM:SS`)
   - Le timestamp ISO complet

### Dans la console du navigateur

Au démarrage de l'application, les informations de version sont automatiquement affichées dans la console :

```
🎁 WishGiftHub
Version: 1.0.0
Déployé le: 2025-12-09 17:10:58
Timestamp: 2025-12-09T16:10:58.960Z
```

## Commandes

### Générer manuellement la version

```bash
npm run generate-version
# ou
node scripts/generate-version.js
```

### Build avec génération automatique

```bash
npm run build
# La version est automatiquement générée avant le build
```

### Développement avec génération automatique

```bash
npm run dev
# La version est automatiquement générée avant le démarrage du serveur de dev
```

## Format de la date

- **Format lisible** : `2025-12-09 17:10:58` (année-mois-jour heure:minute:seconde)
- **Format ISO** : `2025-12-09T16:10:58.960Z` (UTC)

## Exemples d'utilisation

### Dans un composant Vue

```typescript
import { onMounted, ref } from 'vue'

interface VersionInfo {
  buildTimestamp: string
  buildDate: string
  version: string
}

const versionInfo = ref<VersionInfo | null>(null)

onMounted(async () => {
  const response = await fetch('/version.json')
  versionInfo.value = await response.json()
  console.log('Version:', versionInfo.value.buildDate)
})
```

### Import TypeScript

```typescript
import { VERSION_INFO } from '@/version'

console.log('Build date:', VERSION_INFO.buildDate)
console.log('Version:', VERSION_INFO.version)
```

## Notes

- La date/heure reflète le moment exact où le build a été exécuté
- Le timezone affiché est celui de la machine qui effectue le build
- Le timestamp ISO est toujours en UTC
- Les fichiers de version sont automatiquement régénérés à chaque build

