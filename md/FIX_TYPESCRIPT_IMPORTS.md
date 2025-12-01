# Fix : Erreurs d'import TypeScript dans les fichiers générés

## 🐛 Problème

Lors du build de production, TypeScript générait des erreurs d'import :

```
"AuthRequest" is not exported by "src/generated/api/wish/data-contracts.ts"
"RequestParams" is not exported by "src/generated/api/wish/http-client.ts"
...
```

Et des erreurs concernant `verbatimModuleSyntax` :

```
error TS1484: 'AuthRequest' is a type and must be imported using a type-only import when 'verbatimModuleSyntax' is enabled.
```

---

## 🔍 Cause

Le générateur `swagger-typescript-api` a généré les fichiers avec :

1. **`@ts-nocheck`** dans l'en-tête des fichiers
   - Cette directive désactive la vérification TypeScript en dev
   - Mais lors du build de production avec `vue-tsc`, elle est ignorée

2. **Imports incorrects pour `verbatimModuleSyntax`**
   - Les types étaient importés avec `import { Type }` au lieu de `import type { Type }`
   - TypeScript avec `verbatimModuleSyntax: true` exige `import type` pour les types purs

---

## ✅ Solution appliquée

### 1. Suppression des directives `@ts-nocheck`

**Fichiers modifiés :**
- `src/generated/api/wish/data-contracts.ts`
- `src/generated/api/wish/Api.ts`
- `src/generated/api/wish/http-client.ts`

**Avant :**
```typescript
/* eslint-disable */
/* tslint:disable */
// @ts-nocheck  ← SUPPRIMÉ
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ...
```

**Après :**
```typescript
/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ...
```

### 2. Correction des imports dans `Api.ts`

**Avant :**
```typescript
import {
  AuthRequest,
  AuthResponse,
  ErrorResponse,
  GroupRequest,
  GroupResponse,
  InvitationRequest,
  InvitationResponse,
  UserResponse,
  WishRequest,
  WishResponse,
} from "./data-contracts";
import {ContentType, HttpClient, RequestParams} from "./http-client";
```

**Après :**
```typescript
import type {
  AuthRequest,
  AuthResponse,
  ErrorResponse,
  GroupRequest,
  GroupResponse,
  InvitationRequest,
  InvitationResponse,
  UserResponse,
  WishRequest,
  WishResponse,
} from "./data-contracts";
import type { RequestParams } from "./http-client";
import { ContentType, HttpClient } from "./http-client";
```

**Changements :**
- Tous les types (interfaces) → `import type { ... }`
- Les classes/enums (`ContentType`, `HttpClient`) → `import { ... }` normal

---

## 🎯 Résultat

### ✅ Type-check passe
```bash
npm run type-check
# ✓ Aucune erreur
```

### ✅ Build de production réussit
```bash
npm run build
# ✓ Compilation réussie
```

### ✅ Application fonctionne
- Pas de régression fonctionnelle
- Les imports TypeScript sont corrects
- Compatible avec `verbatimModuleSyntax: true`

---

## 📝 Notes importantes

### À chaque régénération de l'API

Lorsque vous exécutez `npm run generate-api`, le générateur va **régénérer les fichiers avec `@ts-nocheck`** et les imports incorrects.

**Il faudra réappliquer les correctifs :**

1. Supprimer les `@ts-nocheck` dans les 3 fichiers
2. Corriger les imports dans `Api.ts`

### Script de post-génération (recommandé)

Pour automatiser ces corrections, vous pouvez créer un script :

**`scripts/fix-generated-api.js` :**
```javascript
import { readFileSync, writeFileSync } from 'fs';

const files = [
  'src/generated/api/wish/data-contracts.ts',
  'src/generated/api/wish/Api.ts',
  'src/generated/api/wish/http-client.ts'
];

// Supprimer @ts-nocheck
files.forEach(file => {
  let content = readFileSync(file, 'utf8');
  content = content.replace('// @ts-nocheck\n', '');
  writeFileSync(file, content, 'utf8');
});

// Fixer les imports dans Api.ts
let apiContent = readFileSync('src/generated/api/wish/Api.ts', 'utf8');
apiContent = apiContent.replace(
  /import \{([^}]+)\} from "\.\/data-contracts";/,
  'import type {$1} from "./data-contracts";'
);
apiContent = apiContent.replace(
  /import \{ContentType, HttpClient, RequestParams\} from "\.\/http-client";/,
  'import type { RequestParams } from "./http-client";\nimport { ContentType, HttpClient } from "./http-client";'
);
writeFileSync('src/generated/api/wish/Api.ts', apiContent, 'utf8');

console.log('✅ API générée corrigée');
```

**Puis dans `package.json` :**
```json
{
  "scripts": {
    "generate-api": "sta generate ... && node scripts/fix-generated-api.js"
  }
}
```

---

## 🔧 Alternative : Configuration du générateur

Une autre solution serait de configurer `swagger-typescript-api` pour ne pas générer `@ts-nocheck`.

Cependant, cela nécessiterait de vérifier la documentation du générateur et pourrait ne pas être supporté.

---

## ✅ Problème résolu

Les erreurs de compilation TypeScript sont maintenant **complètement corrigées**. Le projet compile sans erreur en dev et en production.

