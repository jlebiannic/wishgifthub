# 🧪 Guide de Test - Page d'Accueil WishGiftHub

## ✅ Tests de Validation Effectués

### 1. Compilation TypeScript
```bash
cd wishgifthub-ui
npm run type-check
```
**Résultat** : ✅ Aucune erreur de typage

### 2. Linting
```bash
npm run lint
```
**Résultat** : ✅ Code conforme aux standards

## 🚀 Instructions de Démarrage

### Prérequis
- Node.js (version 18+)
- npm

### Installation et Démarrage

```bash
# 1. Se positionner dans le dossier UI
cd wishgifthub-ui

# 2. Installer les dépendances (si pas déjà fait)
npm install

# 3. Vérifier la configuration
cat .env
# Doit contenir : VITE_API_URL=http://localhost:8080

# 4. Démarrer le serveur de développement
npm run dev

# L'application sera accessible sur http://localhost:5173
```

## 🧪 Scénarios de Test

### Scénario 1 : Page d'accueil (Non connecté)

**Étapes** :
1. Ouvrir http://localhost:5173
2. Vérifier l'affichage du titre "Bienvenue sur WishGiftHub"
3. Vérifier la présence du formulaire de connexion
4. Vérifier le message "La connexion est réservée aux administrateurs"
5. Vérifier le lien "En savoir plus sur les rôles"

**Résultat attendu** :
- ✅ Titre avec icône cadeau
- ✅ Formulaire centré
- ✅ Alerte d'information visible
- ✅ Design responsive

### Scénario 2 : Connexion Administrateur

**Note** : Ce scénario nécessite que le backend soit démarré sur http://localhost:8080

**Étapes** :
1. Entrer un identifiant admin valide
2. Entrer le mot de passe
3. Cliquer sur "Se connecter"

**Résultat attendu** :
- ✅ Redirection vers le dashboard
- ✅ Badge "Administrateur" affiché
- ✅ Bouton "Créer un groupe" visible
- ✅ Liste des groupes affichée

### Scénario 3 : Affichage des Invitations (Admin)

**Prérequis** : Être connecté en tant qu'admin avec au moins un groupe

**Étapes** :
1. Cliquer sur l'icône "œil" d'un groupe
2. Observer le dialog qui s'ouvre

**Résultat attendu** :
- ✅ Dialog modal affiché
- ✅ Liste des invitations avec statuts
- ✅ Couleurs différenciées (vert/orange/rouge)
- ✅ Bouton "Fermer" fonctionnel

### Scénario 4 : Création de Groupe (Admin)

**Prérequis** : Être connecté en tant qu'admin

**Étapes** :
1. Cliquer sur "Créer un groupe"
2. Entrer un nom de groupe
3. Entrer une description (optionnel)
4. Cliquer sur "Créer"

**Résultat attendu** :
- ✅ Dialog de création affiché
- ✅ Formulaire validé
- ✅ Groupe ajouté à la liste
- ✅ Dialog fermé automatiquement

### Scénario 5 : Déconnexion

**Prérequis** : Être connecté

**Étapes** :
1. Cliquer sur le bouton "Déconnexion"

**Résultat attendu** :
- ✅ Retour à la page de connexion
- ✅ Session effacée
- ✅ Groupes non visibles

### Scénario 6 : Gestion des Erreurs

**Test 6a : Identifiants incorrects**
1. Entrer des identifiants invalides
2. Cliquer sur "Se connecter"

**Résultat attendu** :
- ✅ Message d'erreur affiché
- ✅ Pas de redirection

**Test 6b : Champs vides**
1. Laisser les champs vides
2. Cliquer sur "Se connecter"

**Résultat attendu** :
- ✅ Validation HTML5 activée
- ✅ Message "Ce champ est requis"

### Scénario 7 : Responsive Design

**Étapes** :
1. Ouvrir l'application en mode desktop
2. Redimensionner la fenêtre (mode tablette)
3. Redimensionner en mode mobile

**Résultat attendu** :
- ✅ Layout adapté à chaque taille d'écran
- ✅ Boutons et cartes dimensionnés correctement
- ✅ Dialogs responsive

### Scénario 8 : Thème Clair/Sombre

**Étapes** :
1. Cliquer sur l'icône de thème dans l'en-tête
2. Observer le changement

**Résultat attendu** :
- ✅ Bascule entre thème clair et sombre
- ✅ Couleurs adaptées
- ✅ Lisibilité maintenue

## 🔧 Tests avec Mock (Sans Backend)

Pour tester l'interface sans backend, vous pouvez modifier temporairement les stores pour retourner des données mockées :

### Mock du Store Auth

```typescript
// Dans src/stores/auth.ts - fonction login
async function login(username: string, password: string) {
  isLoading.value = true
  error.value = null

  // MOCK - à supprimer en production
  await new Promise(resolve => setTimeout(resolve, 1000)) // Simule latence
  
  user.value = {
    id: '1',
    username: username,
    email: 'admin@wishgifthub.com',
    roles: ['ADMIN']
  }
  token.value = 'mock-token-123'
  
  localStorage.setItem('auth_token', 'mock-token-123')
  localStorage.setItem('user', JSON.stringify(user.value))
  
  isLoading.value = false
  return true
}
```

### Mock du Store Group

```typescript
// Dans src/stores/group.ts - fonction fetchMyGroups
async function fetchMyGroups() {
  isLoading.value = true
  
  // MOCK
  await new Promise(resolve => setTimeout(resolve, 500))
  
  groups.value = [
    {
      id: '1',
      name: 'Famille Dupont',
      description: 'Groupe familial pour les fêtes',
      createdBy: 'admin'
    },
    {
      id: '2',
      name: 'Amis',
      description: 'Groupe d\'amis',
      createdBy: 'admin'
    }
  ]
  
  isLoading.value = false
}
```

## 📊 Checklist de Validation Complète

### Interface Utilisateur
- [ ] Titre et icône présents
- [ ] Formulaire de connexion centré
- [ ] Champs avec icônes appropriées
- [ ] Message d'information sur l'accès admin
- [ ] Lien "En savoir plus" présent
- [ ] Toggle thème fonctionnel

### Fonctionnalités Admin
- [ ] Badge "Administrateur" visible
- [ ] Bouton "Créer un groupe" présent
- [ ] Dialog de création fonctionnel
- [ ] Icône "œil" sur chaque groupe
- [ ] Dialog invitations s'ouvre correctement
- [ ] Statuts colorés dans le dialog
- [ ] Bouton déconnexion fonctionnel

### Fonctionnalités Utilisateur
- [ ] Liste des groupes affichée
- [ ] Pas de bouton "Créer un groupe"
- [ ] Pas d'icône "œil"
- [ ] Déconnexion fonctionne

### Gestion d'État
- [ ] Session persistante (refresh page)
- [ ] Déconnexion efface la session
- [ ] Loading states affichés
- [ ] Erreurs gérées et affichées

### Performance
- [ ] Pas d'erreurs console
- [ ] Pas de warnings TypeScript
- [ ] Temps de chargement acceptable
- [ ] Pas de fuites mémoire

### Responsive
- [ ] Desktop (>1200px) : OK
- [ ] Tablette (768-1200px) : OK
- [ ] Mobile (<768px) : OK

## 🐛 Dépannage

### Problème : Le serveur ne démarre pas
```bash
# Supprimer node_modules et réinstaller
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Problème : Erreur CORS avec l'API
Vérifier que le backend autorise les requêtes depuis http://localhost:5173

### Problème : Variables d'environnement non chargées
- Vérifier que le fichier `.env` existe
- Redémarrer le serveur de développement
- Les variables doivent commencer par `VITE_`

## 📝 Rapport de Test

**Date** : 18 novembre 2025  
**Testeur** : Développeur  
**Version** : 1.0.0

| Scénario | Statut | Commentaires |
|----------|--------|--------------|
| Compilation TypeScript | ✅ Pass | Aucune erreur |
| Linting | ✅ Pass | Code conforme |
| Interface accueil | ⏳ À tester | Nécessite serveur dev |
| Connexion admin | ⏳ À tester | Nécessite backend |
| Affichage invitations | ⏳ À tester | Nécessite backend |
| Création groupe | ⏳ À tester | Nécessite backend |
| Déconnexion | ⏳ À tester | Nécessite serveur dev |
| Gestion erreurs | ⏳ À tester | Nécessite backend |
| Responsive | ⏳ À tester | Nécessite serveur dev |
| Thème clair/sombre | ⏳ À tester | Nécessite serveur dev |

**Prochaines actions** :
1. Démarrer le serveur de développement
2. Exécuter les tests manuels
3. Connecter au backend
4. Tester les scénarios complets

