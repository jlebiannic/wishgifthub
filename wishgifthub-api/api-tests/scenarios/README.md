# 🧪 Scénario de Non-Régression WishGiftHub

## 📋 Description

Ce scénario de tests automatisés couvre l'ensemble des fonctionnalités principales de l'application WishGiftHub. Il permet de vérifier que toutes les APIs fonctionnent correctement après chaque modification du code.

## 🎯 Objectif

Valider automatiquement :
- ✅ L'authentification et la création d'admin
- ✅ La gestion des groupes (création, modification, suppression)
- ✅ Le système d'invitations
- ✅ La gestion des membres
- ✅ La gestion des souhaits (création, réservation, annulation, suppression)
- ✅ Les contrôles de sécurité (permissions, restrictions)

## 📂 Structure du scénario

Le scénario est composé de **17 étapes** dans le dossier `api-tests/scenarios/` :

### Phase 1 : Configuration (Étapes 1-3)
1. **scenario-non-regression.bru** - Inscription d'un administrateur
2. **2-create-group.bru** - Création d'un groupe "Noël en famille 2025"
3. **3-list-admin-groups.bru** - Liste des groupes de l'admin

### Phase 2 : Invitations et Membres (Étapes 4-7)
4. **4-create-invitation.bru** - Création d'une invitation pour un user
5. **5-accept-invitation.bru** - Acceptation de l'invitation par le user
6. **6-user-list-groups.bru** - Le user liste ses groupes
7. **7-list-group-members.bru** - Consultation des membres du groupe

### Phase 3 : Gestion des Souhaits (Étapes 8-13)
8. **8-user-add-wish.bru** - Le user ajoute un souhait (PS5)
9. **9-list-my-wishes.bru** - Le user liste ses propres souhaits
10. **10-admin-reserve-wish.bru** - L'admin réserve le souhait du user
11. **11-admin-list-all-wishes.bru** - L'admin liste tous les souhaits du groupe
12. **12-admin-unreserve-wish.bru** - L'admin annule sa réservation
13. **13-user-delete-wish.bru** - Le user supprime son souhait

### Phase 4 : Modifications et Sécurité (Étapes 14-17)
14. **14-admin-update-group.bru** - L'admin modifie le nom du groupe
15. **15-security-user-cannot-update-group.bru** - Test : un user ne peut pas modifier un groupe
16. **16a-create-wish-for-security-test.bru** - Création d'un souhait pour test de sécurité
17. **16b-security-user-cannot-reserve-own-wish.bru** - Test : un user ne peut pas réserver son propre souhait
18. **17-admin-delete-group.bru** - L'admin supprime le groupe (nettoyage)

## 🚀 Exécution

### Prérequis
1. L'application backend doit être démarrée (`http://localhost:8080`)
2. Bruno CLI doit être installé (`npm install -g @usebruno/cli`)

### Méthode 1 : Via Bruno Desktop
1. Ouvrir Bruno Desktop
2. Ouvrir le dossier `api-tests`
3. Sélectionner l'environnement `local`
4. Cliquer sur le dossier `scenarios`
5. Cliquer sur "Run Folder" pour exécuter tous les tests dans l'ordre

### Méthode 2 : Via Bruno CLI
```bash
cd api-tests
bru run scenarios --env local
```

### Exécution d'une seule étape
```bash
# Exemple : tester uniquement la création de groupe
bru run scenarios/2-create-group.bru --env local
```

## 📊 Variables d'environnement utilisées

Le scénario utilise les variables suivantes (auto-générées pendant l'exécution) :

| Variable | Description | Générée à l'étape |
|----------|-------------|-------------------|
| `admin_token` | JWT de l'admin | Étape 1 |
| `group_id` | UUID du groupe créé | Étape 2 |
| `invitation_token` | UUID du token d'invitation | Étape 4 |
| `user_token` | JWT du user invité | Étape 5 |
| `wish_id` | UUID du souhait créé | Étape 8 |
| `wish_id_for_security_test` | UUID pour test de sécurité | Étape 16a |

## ✅ Résultats attendus

Si tout fonctionne correctement :
- **Toutes les étapes** doivent retourner un statut HTTP de succès (200, 204)
- **Les tests de sécurité** (étapes 15 et 16b) doivent échouer avec 403 ou 400 (comportement attendu)
- **Les logs** affichent ✅ pour chaque étape réussie

Exemple de sortie console :
```
✅ ÉTAPE 1: Inscription admin réussie
✅ ÉTAPE 2: Création groupe réussie - ID: xxx
✅ ÉTAPE 3: Liste des groupes récupérée - 1 groupe(s)
...
✅ ÉTAPE 17: Admin a supprimé le groupe
====================================
✅ SCÉNARIO DE NON-RÉGRESSION TERMINÉ AVEC SUCCÈS!
====================================
```

## 🔧 Personnalisation

Pour adapter le scénario à vos besoins :

1. **Modifier les données de test** : Changez les emails, noms de groupes, etc. dans chaque fichier `.bru`
2. **Ajouter des étapes** : Créez de nouveaux fichiers `.bru` avec un numéro de séquence (`seq`) approprié
3. **Modifier l'environnement** : Éditez `environments/local.bru` pour changer l'URL de base

## 🐛 Dépannage

### Erreur : "admin_token is undefined"
- **Cause** : L'étape 1 a échoué ou n'a pas été exécutée
- **Solution** : Vérifiez que l'application backend est bien démarrée et accessible

### Erreur : "group_id is undefined"
- **Cause** : L'étape 2 a échoué
- **Solution** : Vérifiez les logs de l'étape 2 et que l'admin_token est valide

### Erreur 403 Forbidden sur une étape normale
- **Cause** : Le JWT est expiré ou invalide
- **Solution** : Relancez tout le scénario depuis l'étape 1

### Tests de sécurité en échec
- **Cause** : Les contrôles de sécurité ne fonctionnent pas correctement
- **Solution** : Vérifiez les annotations `@PreAuthorize` dans les contrôleurs

## 📝 Notes importantes

- ⚠️ Le scénario crée et supprime des données en base. Ne l'exécutez **PAS** sur un environnement de production
- 🔄 Le scénario est idempotent : vous pouvez le relancer plusieurs fois
- 🧹 La suppression du groupe (étape 17) nettoie automatiquement les données (cascade)
- 📧 Les emails utilisés sont fictifs (`admin.test@example.com`, `user.invite@example.com`)

## 🎓 Bonnes pratiques

1. **Exécutez le scénario** après chaque modification importante du code
2. **Ajoutez de nouveaux tests** pour chaque nouvelle fonctionnalité
3. **Vérifiez les logs** pour comprendre les échecs
4. **Gardez les tests à jour** avec l'évolution de l'API

## 📞 Support

En cas de problème avec les tests :
1. Vérifiez que l'application backend est démarrée
2. Consultez les logs de l'application Spring Boot
3. Vérifiez la configuration de l'environnement Bruno (`environments/local.bru`)
4. Consultez la documentation de Bruno : https://docs.usebruno.com/

