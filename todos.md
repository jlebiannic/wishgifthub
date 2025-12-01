# Fonc
- ajouter pseudo et avatar à la création de l'invitation
- possibilité de modifier le pseudo et l'avatar
- liens amazon à revoir (images)
- bouton réservé par moi tronqué sur smartphone
- bouton ajouter un souhait tronqué/peu visible sur smartphone
- si url ou url image invalide afficher une erreur 
- ajout d'un bouton pour rafraichir le groupe

# Tech
- Les apis générées coté front ont été modifiées, il faut trouver un moyen de les générer un fois pour toute sna modif manuelle
- ajouter la gestion des erreurs globales
- refresh auto des souhaits

# ✅ Terminé (2025-12-01)
- ✅ Affichage de qui a réservé un souhait (nom extrait de l'email)
- ✅ Suppression de souhaits (bouton corbeille sur mes souhaits)
- ✅ Support des favoris navigateur et rafraîchissement de page
- ✅ Ajout des champs prix et image aux souhaits
- ✅ Extraction automatique de métadonnées depuis URL (Jsoup)
- ✅ Configuration Flyway pour la gestion des migrations DB
- ✅ Bouton "Réserver" masqué si déjà réservé
- ✅ Bouton "Annuler" renommé en "Annuler la réservation"
- ✅ Fix taille des colonnes VARCHAR (urls longues)
- ✅ Validation frontend des champs (limites de caractères + compteurs)
- ✅ Gestion des conflits de réservation (erreur 409 + message + rafraîchissement auto)
- ✅ Remplacer les alerts par des messages plus user friendly pour les conflits de réservation

