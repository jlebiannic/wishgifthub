# A faire

## Fonc à faire
- ajouter pseudo et avatar à la création de l'invitation
- possibilité de modifier le pseudo et l'avatar
- possibilité de se déconnecter sur l'icone user en haut à droite
- ajout d'un bouton pour rafraichir le groupe
- reserré l'écran (bandes à droite et à gauche)
- formulaire de contact
- tout déplier/replier
- page tableau de bord notamment pour voir les souhaits réservés de qui et mes souhaits réservés par qui
- Un membre peu créer un membre enfant ou autre.
- Réserver à plusieurs (ex: 2 personnes réservent un cadeau commun)

## Tech à faire
- Revoir le mode admin car il est conditionné uniquement par un boolean et donc admin=true veut dire admin de n'importe quel groupe (il faut ajouter une colonne id admin à la table groupe plutôt)!
- ajouter la gestion des erreurs globales
- refresh auto des souhaits
- renouvellement de la clé de chiffrement jwt
- gestion des erreurs 401/403 globalement
- gestion globale des erreurs front
- gestion de l'expiration du token jwt 

# Fait

## Fonc fait
- avatars plus jolis et variés (48 avatars thématiques: animaux, nature, objets, etc.)
- si déjà réservé et possibilité de réserver alors afficher une erreur au moment du clique et mettre à jour le bouton
- bouton réservé par moi tronqué sur smartphone
- bouton ajouter un souhait tronqué/peu visible sur smartphone
- remplacer les alerts par qq chose de plus user friendly
- Pouvoir modifier un souhait
- si url ou url image invalide afficher une erreur

## Tech fait
- Les apis générées coté front ont été modifiées, il faut trouver un moyen de les générer une fois pour toute sans modif manuelle
- liens amazon à revoir (images)
