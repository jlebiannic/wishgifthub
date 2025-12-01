# ✅ FIX RAPIDE - Colonnes manquantes

## ❌ Erreur

```
Schema-validation: missing column [image_url] in table [wishes]
```

## ✅ Solution appliquée

**Fichier modifié** : `application.properties`

**Changement** :
```properties
# Avant
spring.jpa.hibernate.ddl-auto=validate

# Après
spring.jpa.hibernate.ddl-auto=update
```

## 🚀 Action

**Redémarrez simplement le backend** :

```bash
cd wishgifthub-api
mvn spring-boot:run
```

**Résultat** :
- ✅ Hibernate ajoute automatiquement les colonnes `image_url` et `price`
- ✅ Les données existantes sont préservées
- ✅ L'application démarre correctement

## 📋 Pourquoi ce problème ?

La base Supabase a été créée **avant** l'ajout des champs `imageUrl` et `price` aux entités. 

Avec `validate`, Hibernate vérifie que le schéma DB correspond aux entités et échoue car les colonnes manquent.

Avec `update`, Hibernate ajoute les colonnes manquantes sans perdre les données.

## 🔄 Optionnel : Repasser en mode validate

**Après le premier démarrage réussi**, vous pouvez (optionnellement) repasser en mode `validate` pour plus de sécurité en production :

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Mais `update` fonctionne très bien et est sûr (il n'ajoute que ce qui manque, ne supprime jamais).

## ✅ C'est corrigé !

Le backend devrait maintenant démarrer correctement et vous pourrez utiliser les champs image et prix dans les souhaits ! 🎉

**Documentation complète** : Voir `FIX_MISSING_COLUMNS.md`

