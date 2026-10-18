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

### Cadeaux communs
* Ajout et modification : interrupteur « Autoriser un cadeau commun », désactivé par défaut.
  Explication : « Permettre à plusieurs personnes de se regrouper pour offrir ce cadeau. »
* Lorsqu'il est activé, un champ numérique affiche le maximum de participants (2 par défaut,
  entier minimum 2), avec boutons moins/plus accessibles au clavier et libellés explicites.
* Une carte collective porte le badge « Cadeau commun ». Tous les membres, y compris le destinataire, voient le
  compteur actuel / maximum et les participants. Les autres membres voient « Participer »
  tant qu'il reste une place, leur propre état et « Annuler ma participation »,
  y compris quand le cadeau est complet.
* Le destinataire voit les noms et avatars des participants, le compteur et l'état complet
  dans ses cartes et son tableau de bord, comme les autres membres. L'API fournit ces
  informations sur toutes ses réponses collectives. Il ne peut pas participer à son propre souhait.
  Les réservations individuelles gardent leur visibilité et leur comportement existants.
* Seul le créateur modifie un souhait. La capacité ne peut pas descendre sous le nombre
  de participants actifs. Le retour en individuel est interdit avec plus d'un participant;
  avec un seul, sa réservation est conservée. Un individuel déjà réservé reste non modifiable.

### Contrat et stockage
* `WishRequest` : `collective` (défaut false), `maxParticipants` (défaut 2, ignoré en individuel).
  `WishResponse` ajoute `collective`, `maxParticipants`, `participantCount`, `participating`, `full`
  et `participants` (liste de `UserResponse` pour les cadeaux communs, sans données d'authentification).
  Ces champs sont visibles aussi par le destinataire ; `participating` vaut alors false.
* Les endpoints existants `POST .../reserve` et `DELETE .../reserve` gèrent aussi les
  participations, avec les mêmes droits de groupe, erreurs métier HTTP 409 et validation HTTP 400.
* La migration Flyway V6 ajoute le mode et la capacité (anciens souhaits : individuel, capacité 1)
  et `wish_participants` (clé unique souhait/utilisateur, suppressions en cascade).
  Les anciennes réservations `reserved_by` sont conservées sans transformation.
* Réserver, annuler, modifier et supprimer utilisent une transaction et un verrou
  pessimiste sur la ligne du souhait pour éviter les dépassements concurrents de capacité.

### Vérification
* Backend : `mvn -q -pl wishgifthub-api -am test`.
* UI (depuis `wishgifthub-ui`) : `npm run test:unit -- --run`, `npm run type-check`,
  `npm run build-only`. Régénération du client : `npm run generate-api` après compilation Maven.
* Les tests de service utilisent H2 PostgreSQL-mode, de vraies transactions/verrous et
  deux réservations simultanées pour la dernière place. Le test de migration exécute V1–V6
  (seul l'ordre DEFAULT/PRIMARY KEY de V1 est adapté à H2). Cela ne remplace pas
  une validation de déploiement sur PostgreSQL.
