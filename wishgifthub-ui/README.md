# wishgifthub-ui

This template should help get you started developing with Vue 3 in Vite.

## Profils enfants

Dans un groupe, **Gérer mes enfants** permet aux administrateurs comme aux invités
de créer autant de profils enfants que nécessaire, puis de modifier leur pseudo
et leur avatar ou de les supprimer. Un enfant rejoint uniquement le groupe courant.
La suppression est définitive et concerne son profil, ses souhaits et ses appartenances
à **tous** les groupes ; une confirmation explicite est demandée.

Dans le groupe, les fiches enfants apparaissent juste sous celle de leur parent,
avec un retrait visuel, sans libellé supplémentaire.

Le bouton **Ajouter un souhait** sur sa propre carte ou celle de son enfant ouvre
le formulaire pour la personne concernée. Les cartes de ses enfants permettent aussi
de modifier et supprimer leurs souhaits. La connexion reste toujours celle du parent :
les réservations et participations sont faites en son nom, jamais au nom de l’enfant.
Les enfants n’ont ni adresse e-mail, ni mot de passe. Les paramètres du profil
personnel du parent restent inchangés.

Le client API généré doit inclure les endpoints `getChildren`, `createChild`,
`updateChild`, `deleteChild` et le champ optionnel `WishRequest.userId`.

### Déploiement

La migration Flyway `V7__child_profiles.sql` s’exécute automatiquement au démarrage
du backend. Elle ajoute la clé étrangère nullable `parent_id` et autorise l’absence
d’adresse e-mail uniquement pour les profils enfants sans mot de passe et non
administrateurs. Les utilisateurs, souhaits et réservations existants sont préservés.

## Suggestion de cadeau commun

Chaque groupe dispose d'un seuil de suggestion, fixé à **50 €** par défaut.
Son administrateur peut le choisir à la création ou le modifier depuis
**Paramètres du groupe**, dans la page du groupe (montant positif ou nul,
deux décimales au maximum).

Lors de l'ajout ou de la modification d'un souhait, un prix strictement supérieur
au seuil déclenche une proposition de cadeau commun. **Oui, cadeau commun**
active les contrôles existants du nombre de participants ; **Non, garder individuel**
permet de conserver le mode individuel. Les prix préremplis par l'extraction de
métadonnées suivent la même règle. Un prix vide, non numérique, une fourchette
ou une devise autre que l'euro ne déclenche pas de suggestion.

## Souhaits provisoires

Les formulaires d'ajout et de modification proposent **Souhait provisoire
(non visible par les autres membres)**, désactivé par défaut. Le souhait reste
visible uniquement par son propriétaire et, pour un enfant, par son parent
gestionnaire membre du groupe. Les autres membres, y compris l'administrateur
du groupe, ne reçoivent pas ces souhaits dans les réponses de l'API.

Un badge indique les souhaits provisoires à leur gestionnaire. Ils ne sont pas
réservables ; décocher l'option les publie et rétablit les actions de réservation.
Un souhait déjà réservé ou ayant des participants ne peut pas être rendu
provisoire afin de préserver les engagements en cours.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd) 
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
