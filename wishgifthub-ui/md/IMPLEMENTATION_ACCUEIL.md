# Implémentation de la Page d'Accueil - WishGiftHub UI

## 📋 Vue d'ensemble

L'implémentation de la page d'accueil suit fidèlement les spécifications définies dans `spec_accueil.md`. L'application utilise **Vue 3** avec **TypeScript** et **Vuetify 3** pour les composants UI.

## 🏗️ Architecture

### Stores (Pinia)

#### **auth.ts**
Store gérant l'authentification et l'état utilisateur :
- Connexion/déconnexion
- Gestion du token JWT
- Persistance de session via localStorage
- Détection du rôle administrateur

#### **group.ts**
Store gérant les groupes et invitations :
- Récupération des groupes de l'utilisateur
- Récupération des invitations d'un groupe
- Création de nouveaux groupes

### Composants

#### **LoginForm.vue**
Formulaire de connexion avec :
- Champs identifiant et mot de passe
- Message d'information sur l'accès réservé aux admins
- Gestion des erreurs
- Lien "En savoir plus sur les rôles"

#### **CreateGroupButton.vue**
Bouton et dialog de création de groupe :
- Visible uniquement pour les administrateurs
- Formulaire avec nom et description
- Validation et gestion d'erreurs

#### **GroupCard.vue**
Carte affichant un groupe :
- Nom et description du groupe
- Icône "œil" pour les admins (affichage des invitations)
- Design responsive avec Vuetify

#### **InvitationsDialog.vue**
Dialog affichant les invitations d'un groupe :
- Liste des membres invités
- Statut avec code couleur (Accepté/En attente/Refusé)
- Affichage uniquement pour les admins

### Vues

#### **HomeView.vue**
Page d'accueil principale avec deux états :

**État non connecté :**
- Titre de bienvenue
- Formulaire de connexion
- Message sur l'accès admin

**État connecté :**
- Informations utilisateur avec badge admin
- Bouton "Créer un groupe" (admin uniquement)
- Liste des groupes de l'utilisateur
- Bouton de déconnexion

## 🎨 Fonctionnalités UX

### Affichage Conditionnel
- Le bouton "Créer un groupe" n'apparaît que pour les administrateurs
- L'icône "œil" sur les groupes n'est visible que pour les administrateurs
- Messages adaptés selon l'état de connexion

### Feedback Utilisateur
- Messages d'erreur clairs et visibles
- Indicateurs de chargement pendant les requêtes
- Messages informatifs quand aucun groupe n'existe
- Validation des formulaires

### Design Responsive
- Interface adaptée desktop et mobile
- Utilisation des composants Vuetify responsive
- Cards et dialogs avec tailles maximales définies

## 🔒 Sécurité

- Token JWT stocké en localStorage
- Restauration automatique de session au chargement
- Vérification du rôle admin côté client
- Headers d'autorisation sur toutes les requêtes API

## 🚀 Utilisation

### Démarrage du projet
```bash
cd wishgifthub-ui
npm install
npm run dev
```

### URLs des APIs
Les stores utilisent des URLs relatives qui passent par le proxy Vite :
- `POST /api/auth/login` - Connexion
- `GET /api/users/my-groups` - Liste des groupes
- `GET /api/groups/{id}/members` - Invitations d'un groupe
- `POST /api/groups` - Création d'un groupe

### Configuration Proxy
Le proxy est configuré dans `vite.config.ts` :
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

Toutes les requêtes vers `/api/*` sont automatiquement redirigées vers `http://localhost:8080/api/*`.

Voir `CONFIGURATION_PROXY.md` pour plus de détails.

## 📝 Points d'amélioration futurs

- [ ] Ajouter un intercepteur axios pour gérer automatiquement les erreurs 401/403
- [ ] Implémenter la gestion du rafraîchissement du token
- [ ] Ajouter des animations de transition entre les états
- [ ] Implémenter le lien "En savoir plus sur les rôles"
- [ ] Ajouter des tests unitaires avec Vitest
- [ ] Implémenter la pagination pour les listes de groupes et invitations
- [ ] Ajouter un système de notifications toast

## 🎯 Conformité aux spécifications

✅ Page d'accueil avec titre et formulaire de connexion  
✅ Message "La connexion est réservée aux administrateurs"  
✅ Bouton "Créer un groupe" visible uniquement pour admin  
✅ Liste des groupes avec cartes/lignes  
✅ Icône "œil" pour afficher les invitations (admin uniquement)  
✅ Dialog des invitations avec statuts (Accepté/En attente)  
✅ Pas d'accès aux invitations pour les non-admins  
✅ Interface intuitive avec icônes et tooltips  
✅ Navigation simple et feedback utilisateur clair  
✅ Design responsive

## 🔧 Technologies utilisées

- **Vue 3** - Framework JavaScript progressif
- **TypeScript** - Typage statique
- **Vuetify 3** - Bibliothèque de composants Material Design
- **Pinia** - Gestion d'état moderne pour Vue
- **Vue Router** - Routage officiel pour Vue.js

