# ✅ Configuration Flyway - WishGiftHub

## 🎯 Qu'est-ce que Flyway ?

Flyway est un outil de **gestion des migrations de base de données** qui :
- ✅ Versionne le schéma de la base de données
- ✅ Applique automatiquement les migrations au démarrage
- ✅ Garantit la cohérence entre développement et production
- ✅ Permet le rollback et le suivi des changements

---

## 📋 Configuration actuelle

### 1. Dépendances Maven (`pom.xml`)

```xml
<!-- Flyway pour les migrations de base de données -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 2. Configuration Spring (`application.properties`)

```properties
# JPA / Hibernate - Validation uniquement, pas de création de schéma (géré par Flyway)
spring.jpa.hibernate.ddl-auto=validate

# Flyway - Gestion des migrations de base de données
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

#### Explication des paramètres :

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate **ne crée PAS** le schéma, il le **valide** uniquement |
| `spring.flyway.enabled` | `true` | Active Flyway |
| `spring.flyway.baseline-on-migrate` | `true` | Crée une baseline si la DB n'est pas vide |
| `spring.flyway.locations` | `classpath:db/migration` | Où trouver les scripts SQL |
| `spring.flyway.validate-on-migrate` | `true` | Valide les migrations avant de les appliquer |

---

## 📁 Structure des migrations

```
wishgifthub-api/
└── src/main/resources/db/migration/
    ├── V1__initial_schema.sql          ✅ Schéma initial (tables, index)
    ├── V2__add_wish_image_price.sql    ✅ Ajout colonnes image_url et price
    └── V3__future_migration.sql        🔜 Futures migrations
```

### Règles de nommage

**Format** : `V{VERSION}__{DESCRIPTION}.sql`

- `V` majuscule obligatoire
- `{VERSION}` : Numéro incrémental (1, 2, 3, ...)
- `__` : Double underscore obligatoire
- `{DESCRIPTION}` : Description snake_case
- `.sql` : Extension SQL

**Exemples valides** :
- ✅ `V1__initial_schema.sql`
- ✅ `V2__add_wish_image_price.sql`
- ✅ `V3__add_user_preferences.sql`

**Exemples invalides** :
- ❌ `v1__schema.sql` (v minuscule)
- ❌ `V1_schema.sql` (un seul underscore)
- ❌ `V1.1__schema.sql` (version décimale non supportée)

---

## 🚀 Démarrage avec Flyway

### Première exécution

Au premier démarrage du backend :

```bash
cd wishgifthub-api
mvn spring-boot:run
```

**Logs attendus** :
```
INFO  o.f.c.i.d.base.BaseDatabaseType : Database: jdbc:postgresql://...
INFO  o.f.c.i.s.JdbcTableSchemaHistory : Creating Schema History table: "public"."flyway_schema_history"
INFO  o.f.core.internal.command.DbMigrate : Current version of schema "public": << Empty Schema >>
INFO  o.f.core.internal.command.DbMigrate : Migrating schema "public" to version "1 - initial schema"
INFO  o.f.core.internal.command.DbMigrate : Migrating schema "public" to version "2 - add wish image price"
INFO  o.f.core.internal.command.DbMigrate : Successfully applied 2 migrations to schema "public"
```

### Table de suivi Flyway

Flyway crée automatiquement la table `flyway_schema_history` :

```sql
SELECT * FROM flyway_schema_history;
```

| installed_rank | version | description | type | script | checksum | installed_by | installed_on | execution_time | success |
|----------------|---------|-------------|------|--------|----------|--------------|--------------|----------------|---------|
| 1 | 1 | initial schema | SQL | V1__initial_schema.sql | 123456 | postgres | 2025-12-01 | 45 | true |
| 2 | 2 | add wish image price | SQL | V2__add_wish_image_price.sql | 789012 | postgres | 2025-12-01 | 12 | true |

---

## 🔄 Workflow de migration

### Ajouter une nouvelle migration

1. **Créer le fichier SQL** :
   ```bash
   touch src/main/resources/db/migration/V3__add_user_avatar.sql
   ```

2. **Écrire le SQL** :
   ```sql
   -- Migration pour ajouter un avatar aux utilisateurs
   
   ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);
   
   COMMENT ON COLUMN users.avatar_url IS 'URL de l''avatar de l''utilisateur';
   ```

3. **Redémarrer l'application** :
   ```bash
   mvn spring-boot:run
   ```

4. **Flyway applique automatiquement** la migration V3

### Migrations déjà appliquées

⚠️ **IMPORTANT** : Une fois qu'une migration est appliquée (enregistrée dans `flyway_schema_history`), **elle ne peut plus être modifiée**.

Si vous modifiez un fichier déjà migré :
```
ERROR: Migration checksum mismatch for migration version 2
Applied to database : 789012
Resolved locally    : 123999
```

**Solutions** :
1. **Production** : Créer une nouvelle migration (V3) pour corriger
2. **Développement** : Supprimer `flyway_schema_history` et recréer la DB

---

## 🗄️ Contenu des migrations

### V1 - Schéma initial

Crée toutes les tables :
- ✅ `users` - Utilisateurs
- ✅ `groups` - Groupes d'événements
- ✅ `user_groups` - Association users ↔ groups
- ✅ `invitations` - Invitations
- ✅ `wishes` - Souhaits/cadeaux
- ✅ Index de performance
- ✅ Commentaires sur les tables

### V2 - Ajout image et prix

Ajoute aux `wishes` :
- ✅ `image_url` VARCHAR(2048) - URL de l'image du produit
- ✅ `price` VARCHAR(50) - Prix estimé

---

## 🔒 Bonnes pratiques

### ✅ À FAIRE

1. **Toujours utiliser `IF NOT EXISTS`** dans les `CREATE TABLE`
   ```sql
   CREATE TABLE IF NOT EXISTS users (...);
   ```

2. **Toujours utiliser `IF NOT EXISTS`** dans les `ALTER TABLE ADD COLUMN`
   ```sql
   ALTER TABLE wishes ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);
   ```

3. **Tester les migrations localement** avant de déployer

4. **Documenter les migrations** avec des commentaires SQL
   ```sql
   -- Migration V3 : Ajouter support des tags
   COMMENT ON COLUMN wishes.tags IS 'Tags au format JSON';
   ```

5. **Utiliser des transactions** (Flyway le fait automatiquement)

### ❌ À ÉVITER

1. ❌ Modifier une migration déjà appliquée
2. ❌ Supprimer une migration déjà appliquée
3. ❌ Utiliser `spring.jpa.hibernate.ddl-auto=create` (conflits avec Flyway)
4. ❌ Créer des migrations avec des numéros de version en doublon

---

## 🧪 Commandes utiles

### Vérifier l'état des migrations

```bash
mvn flyway:info
```

Affiche :
- ✅ Migrations appliquées
- ⏳ Migrations en attente
- ❌ Migrations échouées

### Réparer Flyway (développement uniquement)

Si `flyway_schema_history` est corrompue :

```bash
mvn flyway:repair
```

### Nettoyer complètement la DB (développement uniquement)

⚠️ **DANGER** : Supprime toutes les données !

```bash
mvn flyway:clean
mvn spring-boot:run  # Réapplique toutes les migrations
```

---

## 🆚 Avant vs Après

### ❌ Avant (sans Flyway)

```properties
spring.jpa.hibernate.ddl-auto=create
```

**Problèmes** :
- 🔴 Schéma recréé à chaque démarrage
- 🔴 Données perdues
- 🔴 Pas de versioning
- 🔴 Impossible de rollback

### ✅ Après (avec Flyway)

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

**Avantages** :
- 🟢 Schéma versionné
- 🟢 Données persistantes
- 🟢 Migrations incrémentales
- 🟢 Traçabilité complète
- 🟢 Déploiements contrôlés

---

## 📊 Résumé

| Aspect | Configuration |
|--------|---------------|
| **Outil** | Flyway 10.x (Spring Boot 3.3) |
| **Base** | PostgreSQL (Supabase) |
| **Migrations** | `src/main/resources/db/migration/` |
| **Hibernate** | Mode `validate` (pas de création) |
| **Auto-migration** | ✅ Au démarrage du backend |
| **Table de suivi** | `flyway_schema_history` |

---

## ✅ Flyway est maintenant configuré !

Au prochain démarrage du backend :
1. Flyway créera `flyway_schema_history`
2. Appliquera V1 (schéma initial)
3. Appliquera V2 (image_url + price)
4. Le backend sera prêt avec la dernière version du schéma

**Vos données seront persistantes et le schéma sera versionné !** 🎉

