# 📘 Cahier des charges – Application Web de Gestion d'Événements

## 🎯 Objectif du projet

Développer une API backend qui gère des groupes d'événements (Noël) et qui permet d'inviter des users par email (via un système externe), de gérer des listes de souhaits, et de réserver des cadeaux.

## 🧱 Architecture technique

- **Back-end** : Spring Boot (Java) (projet distinct)
- **Base de données** : Supabase (PostgreSQL)
- **Authentification** : 
  - Administrateurs : login + mot de passe hashé
  - Users invités : accès via token d'invitation (pas de mot de passe)
- **Sécurisation** : Token JWT pour les administrateurs et les users

## 🧪 Fonctionnalités à implémenter

### 1. Authentification

#### Administrateur
- **Inscription / Connexion**
  - Création de compte administrateur avec email + mot de passe
  - Stockage du mot de passe hashé dans Supabase
  - Endpoint API : `POST /api/auth/register`, `POST /api/auth/login`
  - Authentification via token JWT (stocké côté client)
  - Le token contient : `user_id`, `is_admin: true`
  - Un administrateur est aussi un utilisateur (avec `is_admin = true` en BDD)

#### User invité
- **Acceptation d'invitation**
  - Endpoint API : `GET /api/invite/{token}`
  - Vérifie le token UUID
  - Crée automatiquement un user avec l'email de l'invitation (sans mot de passe)
  - Génère immédiatement un token JWT
  - Le token contient : `user_id`, `is_admin: false`
  - Marque l'invitation comme acceptée

### 2. Gestion des groupes

- **Création d'un groupe** (administrateur uniquement)
  - Champs : nom du groupe, type d'événement (`noël` uniquement pour l'instant)
  - L'administrateur devient propriétaire du groupe
  - L'administrateur est automatiquement ajouté dans `user_groups`
  - Endpoint API : `POST /api/groups`

- **Modification / Suppression d'un groupe** (administrateur uniquement)
  - Seul l'administrateur peut modifier ou supprimer son groupe
  - Suppression en cascade : invitations, souhaits et membres
  - Endpoint API : `PUT /api/groups/{id}`, `DELETE /api/groups/{id}`

- **Liste des groupes de l'administrateur**
  - Endpoint API : `GET /api/groups`

### 3. Invitations (sans envoi d'emails)

- **Création d'une invitation** (administrateur uniquement)
  - Endpoint API : `POST /api/groups/{groupId}/invite`
  - Entrée : `email`
  - Génération d'un `token` UUID
  - Stockage dans la table `invitations`
  - Retourne un lien d'invitation : `https://app.com/join/{token}`

- **Acceptation d'une invitation**
  - Endpoint API : `GET /api/invite/{token}`
  - Vérifie le token et retourne les infos du groupe
  - Crée un user avec l'email de l'invitation
  - Ajoute le user dans `user_groups`
  - Génère et retourne un token JWT
  - Marque l'invitation comme acceptée

### 4. Gestion des utilisateurs (nécessite un token JWT valide)

#### Endpoints des users

- **Consulter ses groupes**
  - `GET /api/users/groups`
  
- **Consulter les membres d'un groupe**
  - `GET /api/groups/{id}/users`
  - Vérifie que l'utilisateur appartient au groupe

- **Gérer ses souhaits**
  - `POST /api/groups/{id}/wishes` - Ajouter un souhait
  - `GET /api/groups/{id}/wishes/me` - Consulter ses propres souhaits
  - `PUT /api/groups/{id}/wishes/{wishId}` - Modifier un de ses souhaits
  - `DELETE /api/groups/{id}/wishes/{wishId}` - Supprimer un de ses souhaits
  - Vérifie que l'utilisateur appartient au groupe

- **Consulter les souhaits des autres**
  - `GET /api/groups/{id}/wishes` - Voir tous les souhaits du groupe
  - Vérifie que l'utilisateur appartient au groupe
  - Les users peuvent voir qui a réservé quel cadeau

- **Réserver un souhait**
  - `POST /api/groups/{id}/wishes/{wishId}/reserve` - Réserver un cadeau
  - `DELETE /api/groups/{id}/wishes/{wishId}/reserve` - Annuler une réservation
  - Vérifie que l'utilisateur appartient au groupe
  - Un user ne peut pas réserver ses propres souhaits

### 5. Base de données (Supabase)

#### Tables principales

##### `users`
- `id` (UUID, PK)
- `email` (VARCHAR, UNIQUE)
- `password_hash` (VARCHAR, NULLABLE - null pour les users invités)
- `is_admin` (BOOLEAN, DEFAULT false)
- `created_at` (TIMESTAMP)

##### `groups`
- `id` (UUID, PK)
- `name` (VARCHAR)
- `type` (VARCHAR - 'noël')
- `admin_id` (UUID, FK → users.id)
- `created_at` (TIMESTAMP)

##### `user_groups`
- `id` (UUID, PK)
- `user_id` (UUID, FK → users.id)
- `group_id` (UUID, FK → groups.id)
- `created_at` (TIMESTAMP)
- UNIQUE (user_id, group_id)

##### `invitations`
- `id` (UUID, PK)
- `user_id` (UUID, FK → users.id, NULLABLE - rempli après acceptation)
- `group_id` (UUID, FK → groups.id)
- `email` (VARCHAR)
- `token` (UUID, UNIQUE)
- `accepted` (BOOLEAN, DEFAULT false)
- `created_at` (TIMESTAMP)

##### `wishes`
- `id` (UUID, PK)
- `user_id` (UUID, FK → users.id - créateur du souhait)
- `group_id` (UUID, FK → groups.id)
- `gift_name` (VARCHAR)
- `description` (TEXT, NULLABLE)
- `url` (VARCHAR, NULLABLE)
- `reserved_by` (UUID, FK → users.id, NULLABLE - user qui a réservé)
- `created_at` (TIMESTAMP)

## 🔐 Sécurité

- Authentification administrateur : email + mot de passe hashé
- Authentification user invité : token UUID d'invitation
- Tous les endpoints protégés par JWT (sauf `/api/auth/*` et `/api/invite/{token}`)
- Le JWT contient : `user_id` et `is_admin`
- Vérification systématique de l'appartenance aux groupes pour les opérations sensibles

## 🔄 Règles métier

- Un user ne peut pas réserver ses propres souhaits
- Les réservations sont visibles par tous les membres du groupe
- Un user peut annuler une réservation (la sienne uniquement)
- Un user peut supprimer ou modifier uniquement ses propres souhaits
- La suppression d'un groupe supprime toutes les données associées (cascade)
- Seul l'administrateur du groupe peut le modifier ou le supprimer

