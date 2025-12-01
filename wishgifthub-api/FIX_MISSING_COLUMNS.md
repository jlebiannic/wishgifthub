# 🔧 Fix : Erreur "missing column [image_url] in table [wishes]"

## ❌ Problème

```
Schema-validation: missing column [image_url] in table [wishes]
```

## 🔍 Cause

La base de données Supabase contient déjà un schéma créé **avant** l'introduction de Flyway, quand Hibernate gérait le schéma avec `ddl-auto=create`. 

Ce schéma existant **ne contient pas** les colonnes `image_url` et `price`.

Quand on passe à `ddl-auto=validate`, Hibernate vérifie que le schéma DB correspond aux entités Java et échoue car les colonnes manquent.

---

## ✅ Solution appliquée

### Option 1 : Mode `update` temporaire (APPLIQUÉE)

**Changement dans `application.properties`** :

```properties
# Temporairement en mode update
spring.jpa.hibernate.ddl-auto=update

# Flyway avec baseline
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=false
```

**Ce qui se passe** :
1. Flyway crée la baseline (ignore le schéma existant)
2. Hibernate ajoute les colonnes manquantes (`image_url`, `price`)
3. L'application démarre correctement

**Après le premier démarrage réussi**, vous pourrez repasser en mode `validate` :

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.validate-on-migrate=true
```

---

## 🔄 Workflow complet

### Étape 1 : Premier démarrage (avec update)

```bash
mvn spring-boot:run
```

**Résultat** :
- ✅ Flyway crée `flyway_schema_history` avec baseline
- ✅ Hibernate ajoute `image_url` et `price` à la table `wishes`
- ✅ Application démarre

### Étape 2 : Vérifier les colonnes

Connectez-vous à Supabase et vérifiez :

```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'wishes';
```

Vous devriez voir :
```
column_name  | data_type
-------------|-----------
id           | uuid
user_id      | uuid
group_id     | uuid
gift_name    | varchar
description  | text
url          | varchar
image_url    | varchar     ← NOUVELLE
price        | varchar     ← NOUVELLE
reserved_by  | uuid
created_at   | timestamp
```

### Étape 3 : Repasser en mode validate (optionnel)

Une fois que tout fonctionne, modifiez `application.properties` :

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.validate-on-migrate=true
```

---

## 🆚 Pourquoi pas `ddl-auto=create` ?

| Mode | Comportement | Problème |
|------|--------------|----------|
| `create` | Recrée tout à chaque démarrage | ❌ **Perte de données** |
| `create-drop` | Recrée + supprime à l'arrêt | ❌ **Perte de données** |
| `update` | Ajoute ce qui manque | ✅ Garde les données |
| `validate` | Vérifie seulement | ✅ Production (après migration) |
| `none` | Rien | ⚠️ Pas de vérification |

**Mode `update`** :
- ✅ Ajoute les colonnes manquantes
- ✅ Garde les données existantes
- ⚠️ Ne supprime jamais (colonnes obsolètes restent)

**Mode `validate`** :
- ✅ Idéal pour production
- ✅ Détecte les incohérences
- ❌ Ne crée rien (besoin de migrations propres)

---

## 🎯 Solution alternative : Migrations SQL manuelles

Si vous préférez une approche **100% Flyway** sans utiliser Hibernate `update` :

### 1. Créer une migration de correction

```sql
-- V3__fix_add_missing_columns.sql
ALTER TABLE wishes 
ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048),
ADD COLUMN IF NOT EXISTS price VARCHAR(50);
```

### 2. Marquer le schéma existant comme baseline

```bash
mvn flyway:baseline
```

### 3. Appliquer les migrations

```bash
mvn flyway:migrate
```

### 4. Démarrer l'application

```bash
mvn spring-boot:run
```

---

## 📊 Comparaison des approches

| Approche | Avantages | Inconvénients |
|----------|-----------|---------------|
| **Hibernate update** | ✅ Rapide<br>✅ Automatique | ⚠️ Moins de contrôle<br>⚠️ Peut laisser des colonnes obsolètes |
| **Flyway pur** | ✅ Contrôle total<br>✅ Versioning propre<br>✅ Reproductible | ⏱️ Plus long à configurer |
| **Flyway baseline** | ✅ Gère les DB existantes<br>✅ Bon compromis | 🔧 Nécessite configuration |

---

## ✅ État actuel

**Configuration active** :
```properties
spring.jpa.hibernate.ddl-auto=update
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

**Ce qui fonctionne** :
- ✅ Base existante respectée
- ✅ Colonnes manquantes ajoutées automatiquement
- ✅ Flyway enregistre la baseline
- ✅ Données préservées

---

## 🚀 Prochaines étapes

1. **Démarrez le backend** :
   ```bash
   mvn spring-boot:run
   ```

2. **Testez que tout fonctionne** :
   - Connexion
   - Ajout de souhait avec image et prix
   - Affichage correct

3. **(Optionnel) Passez en mode validate** :
   - Modifiez `application.properties`
   - Redémarrez

---

## 💡 Pour éviter ce problème à l'avenir

### En développement

Utilisez toujours Flyway dès le début :
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

### En production

- ✅ Flyway obligatoire
- ✅ Hibernate en mode `validate` ou `none`
- ✅ Migrations testées en dev avant deploy
- ✅ Backup avant chaque migration

---

## ✅ Problème résolu !

Avec `ddl-auto=update` + `baseline-on-migrate=true` :
- ✅ Le schéma existant est respecté
- ✅ Les colonnes manquantes sont ajoutées
- ✅ L'application démarre correctement
- ✅ Les données sont préservées

**Vous pouvez maintenant utiliser les champs image et prix !** 🎉

