# 🧪 Collection Bruno - WishGiftHub API

Cette collection contient tous les tests pour l'API WishGiftHub.

## Profils enfants : tests automatisés

Depuis la racine du dépôt :

```powershell
mvn -pl wishgifthub-api "-Dtest=ChildApiTest,ChildMigrationTest" test
```

Ces tests couvrent la création de plusieurs enfants depuis un groupe par un membre
invité, la modification des profils et de leurs souhaits, les restrictions au parent
créateur, les réservations et la suppression des données de l'enfant. Ils vérifient
aussi que la migration V7 conserve les comptes, souhaits et réservations existants.
La migration est exécutée automatiquement par Flyway au démarrage de l'API.

## Seuil de suggestion de cadeau commun : tests automatisés

Depuis la racine du dépôt :

```powershell
mvn -pl wishgifthub-api "-Dtest=GroupThresholdApiTest,GroupThresholdMigrationTest" test
```

Le champ optionnel `collectiveGiftThreshold` configure le seuil en EUR par groupe :
de `0` à `99999999.99`, avec au plus deux décimales. À la création, son omission
utilise `50.00` ; à la modification, elle conserve la valeur existante.
Seul l'administrateur propriétaire peut modifier le groupe. Ce seuil sert uniquement
à proposer un cadeau commun et ne l'impose jamais aux souhaits.

Flyway applique automatiquement `V8__group_collective_gift_threshold.sql` au démarrage.
Cette migration ajoute une colonne `NUMERIC(10, 2)` non nulle, avec `50.00` pour les
groupes existants et par défaut pour les nouveaux, sans modifier les comptes,
enfants, appartenances, invitations, souhaits, réservations ou participants existants.
Les tests couvrent ces garanties, les valeurs personnalisées, la conservation du
seuil à la modification, les autorisations et le rejet des valeurs invalides.

## Souhaits provisoires : tests automatisés

```powershell
mvn -pl wishgifthub-api "-Dtest=ProvisionalWishApiTest,ProvisionalWishMigrationTest,ChildApiTest,CollectiveGiftApiTest,CollectiveGiftTest" test
```

`WishRequest.provisional` est optionnel, sans valeur par défaut dans le contrat :
omis à la création il vaut `false`, omis à la modification il conserve sa valeur.
Chaque `WishResponse` retourne ce booléen. Un souhait provisoire n'est visible
que par son destinataire ou son parent gestionnaire membre du même groupe,
sur `/wishes`, `/wishes/me` et `/wishes/users/{userId}`. Aucun accès supplémentaire
n'est accordé aux administrateurs. La gestion reste réservée au destinataire
et au parent gestionnaire.

Passer `provisional` à `false` publie le souhait et autorise les réservations.
Réserver ou annuler une réservation sur un souhait provisoire renvoie `404`
aux autres membres, même avec un UUID connu, et `409` au destinataire/parent.
Rendre provisoire un souhait avec une réservation individuelle ou des participants
collectifs renvoie `409` sans modifier les données, même lors d'un changement
simultané du mode de réservation.

Flyway applique automatiquement `V9__provisional_wishes.sql` : colonne booléenne
`NOT NULL DEFAULT FALSE`, tous les souhaits existants restent publiés. Les tests
couvrent les listes, autorisations, parents/enfants, publication, conservation de
l'état en cas d'omission, conflits et préservation des données lors de la migration.

## 📁 Structure

```
api-tests/
├── bruno.json                    # Configuration de la collection
├── environments/
│   └── local.bru                 # Variables d'environnement
├── auth/
│   ├── register.bru              # Inscription administrateur
│   └── login.bru                 # Connexion administrateur
├── groups/
│   ├── create-group.bru          # Créer un groupe
│   ├── list-groups.bru           # Lister mes groupes
│   ├── update-group.bru          # Modifier un groupe
│   ├── get-group-members.bru     # Voir les membres
│   └── delete-group.bru          # Supprimer un groupe
├── invitations/
│   ├── create-invitation.bru     # Créer une invitation
│   └── accept-invitation.bru     # Accepter une invitation
├── users/
│   └── list-my-groups.bru        # Lister mes groupes
└── wishes/
    ├── create-wish.bru           # Créer un souhait
    ├── list-all-wishes.bru       # Lister tous les souhaits
    ├── reserve-wish.bru          # Réserver un souhait
    ├── unreserve-wish.bru        # Annuler une réservation
    └── delete-wish.bru           # Supprimer un souhait
```

## 🚀 Utilisation

### 1. Importer la collection dans Bruno

1. Ouvrez Bruno
2. Cliquez sur "Open Collection"
3. Sélectionnez le dossier `api-tests`

### 2. Ordre d'exécution recommandé

#### Phase 1 : Configuration administrateur
1. **Register Admin** - Crée un compte administrateur
   - ✅ Sauvegarde automatiquement le `admin_token`
2. **Login Admin** - Se connecte (optionnel si register fonctionne)

#### Phase 2 : Création du groupe
3. **Create Group** - Crée un groupe "Noël 2025"
   - ✅ Sauvegarde automatiquement le `group_id`
4. **List My Groups** - Vérifie la liste des groupes

#### Phase 3 : Invitation d'un utilisateur
5. **Create Invitation** - Invite `user@example.com`
   - ✅ Sauvegarde automatiquement le `invitation_token`
6. **Accept Invitation** - L'utilisateur accepte l'invitation
   - ✅ Sauvegarde automatiquement le `user_token`

#### Phase 4 : Gestion des souhaits
7. **Create Wish** (avec `user_token`) - L'utilisateur crée un souhait
   - ✅ Sauvegarde automatiquement le `wish_id`
8. **List All Wishes** - Voir tous les souhaits du groupe
9. **Reserve Wish** (avec `admin_token`) - L'admin réserve le souhait
10. **Cancel Reservation** (avec `admin_token`) - Annule la réservation

#### Phase 5 : Tests de sécurité
11. **Get Group Members** - Voir les membres du groupe
12. **Update Group** - Modifier le nom du groupe
13. **Delete Wish** (avec `user_token`) - Supprimer le souhait
14. **Delete Group** - ⚠️ Nettoie tout (à faire en dernier)

## 🔑 Variables d'environnement

Les variables suivantes sont automatiquement mises à jour :

| Variable | Description | Sauvegardée par |
|----------|-------------|-----------------|
| `base_url` | URL de l'API | Manuel (http://localhost:8080) |
| `admin_token` | Token JWT admin | Register/Login |
| `user_token` | Token JWT user | Accept Invitation |
| `group_id` | ID du groupe | Create Group |
| `wish_id` | ID du souhait | Create Wish |
| `invitation_token` | Token d'invitation | Create Invitation |

## ✅ Tests automatiques

Chaque requête inclut des tests automatiques :

- Vérification du code de statut HTTP
- Validation de la structure de la réponse
- Vérification des données retournées
- Sauvegarde automatique des variables

## 📝 Notes importantes

### Authentification

- **Administrateur** : Utilise `admin_token` (obtenu via register/login)
- **User invité** : Utilise `user_token` (obtenu via accept invitation)

### Règles de sécurité testées

✅ Un user ne peut pas réserver ses propres souhaits  
✅ Seul le créateur peut supprimer son souhait  
✅ Seul celui qui a réservé peut annuler sa réservation  
✅ Seul l'admin du groupe peut le modifier/supprimer  
✅ Les users doivent appartenir au groupe pour accéder aux ressources

### Endpoints publics (sans authentification)

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/invite/{token}`

### Endpoints protégés (nécessitent un JWT)

Tous les autres endpoints nécessitent un token JWT valide dans le header `Authorization: Bearer {token}`.

## 🐛 Debugging

Si une requête échoue :

1. Vérifiez que l'application Spring Boot est démarrée
2. Vérifiez que la base de données est accessible
3. Vérifiez que les variables d'environnement sont bien remplies
4. Consultez les logs de l'application pour plus de détails

## 🎯 Scénarios de test complets

### Scénario 1 : Flux complet utilisateur
```
1. Register Admin
2. Create Group
3. Create Invitation
4. Accept Invitation (user reçoit son token)
5. Create Wish (avec user_token)
6. List All Wishes (voir son souhait)
7. Reserve Wish (avec admin_token - admin réserve)
8. List All Wishes (vérifier la réservation)
9. Cancel Reservation (admin annule)
10. Delete Wish (user supprime son souhait)
```

### Scénario 2 : Test de sécurité
```
1. Create Wish (avec user_token)
2. Reserve Wish (avec user_token) → ❌ DOIT ÉCHOUER (pas ses propres souhaits)
3. Reserve Wish (avec admin_token) → ✅ OK
4. Cancel Reservation (avec user_token) → ❌ DOIT ÉCHOUER (pas sa réservation)
5. Delete Wish (avec admin_token) → ❌ DOIT ÉCHOUER (pas son souhait)
```

## 📚 Ressources

- [Documentation Bruno](https://www.usebruno.com/docs)
- [Cahier des charges](../CAHIER_DES_CHARGES_API.md)

---

💡 **Astuce** : Utilisez la fonctionnalité "Run Collection" de Bruno pour exécuter tous les tests d'un dossier en séquence.
{
  "version": "1",
  "name": "WishGiftHub API",
  "type": "collection"
}
