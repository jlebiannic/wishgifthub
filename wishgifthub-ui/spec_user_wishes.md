# Spécification IHM / UI – Ajout et visualisation des souhaits

## Page de membres du groupe
* Lorque l'utilisateur ou l'admin clique sur un groupe, alors une nouvelle page s'ouvre et la liste des membrs s'affiche.
* Il s'agit d'une liste de carte contenant un avatar, le nom du membre, et un bouton pour ajouter un souhait (que pour la carte du user connecté). 
* La carte du user connecté vient en premier (elle affiche "moi" et entre parenthèse le nom)

### Visualisattion et réservation des souhaits
* Lorsque l'utilisateur clique sur la carte, la carte s'étend pour afficher la liste des souhaits de ce membre.
* Chaque souhait est affiché avec une image, un titre, une description, une url et un prix.
* Pour chaque souhait, il est possible de le réserver ou d'annuler sa réservation si c'est le user courant qui l'a réservé.
* Un souhait réservé affiche le nom du membre qui l'a réservé.

### Ajout d'un souhait
* Lorsque l'utilisateur clique sur le bouton "Ajouter un souhait", un dialog s'ouvre avec un formulaire d'ajout de souhait.
* Le dialog d'ajout de souhait contient les champs suivants :
  - Champ Url : Si saisi : alimente automatiquement les champs Image, Titre, Description, Prix mais ceux ci restent modifiable manuellement
  - Champ Image
  - Champ Titre
  - Champ Description
  - Champ Prix
