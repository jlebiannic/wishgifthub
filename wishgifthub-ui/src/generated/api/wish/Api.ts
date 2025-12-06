/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

import type {
  AuthRequest,
  AuthResponse,
  ErrorResponse,
  GroupRequest,
  GroupResponse,
  InvitationRequest,
  InvitationResponse,
  MetadataResponse,
  UpdateAvatarRequest,
  UserResponse,
  WishRequest,
  WishResponse,
} from "./data-contracts";
import type { RequestParams } from "./http-client";
import { ContentType, HttpClient } from "./http-client";

export class Api<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Crée un nouveau compte administrateur avec email et mot de passe. Le mot de passe est hashé avant stockage en base de données. Retourne immédiatement un token JWT pour authentification.
   *
   * @tags Authentification
   * @name Register
   * @summary Inscription d'un administrateur
   * @request POST:/api/auth/register
   */
  register = (data: AuthRequest, params: RequestParams = {}) =>
    this.request<AuthResponse, ErrorResponse>({
      path: `/api/auth/register`,
      method: "POST",
      body: data,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Authentifie un administrateur avec son email et mot de passe. Vérifie les identifiants et retourne un token JWT en cas de succès.
   *
   * @tags Authentification
   * @name Login
   * @summary Connexion d'un administrateur
   * @request POST:/api/auth/login
   */
  login = (data: AuthRequest, params: RequestParams = {}) =>
    this.request<AuthResponse, ErrorResponse>({
      path: `/api/auth/login`,
      method: "POST",
      body: data,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de tous les groupes dont l'utilisateur authentifié est administrateur. Nécessite le rôle ADMIN.
   *
   * @tags Groupes
   * @name GetGroups
   * @summary Lister les groupes de l'administrateur
   * @request GET:/api/groups
   * @secure
   */
  getGroups = (params: RequestParams = {}) =>
    this.request<GroupResponse[], ErrorResponse>({
      path: `/api/groups`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Crée un nouveau groupe d'événement. L'administrateur authentifié devient propriétaire du groupe. Il est automatiquement ajouté comme membre du groupe. Un token JWT mis à jour avec l'ID du groupe est retourné.
   *
   * @tags Groupes
   * @name CreateGroup
   * @summary Créer un nouveau groupe
   * @request POST:/api/groups
   * @secure
   */
  createGroup = (data: GroupRequest, params: RequestParams = {}) =>
    this.request<GroupResponse, ErrorResponse>({
      path: `/api/groups`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Supprime définitivement un groupe et toutes ses données associées. Seul l'administrateur propriétaire du groupe peut le supprimer. **Suppression en cascade** : - Tous les liens user_groups - Toutes les invitations - Tous les souhaits
   *
   * @tags Groupes
   * @name DeleteGroup
   * @summary Supprimer un groupe
   * @request DELETE:/api/groups/{groupId}
   * @secure
   */
  deleteGroup = (groupId: string, params: RequestParams = {}) =>
    this.request<void, ErrorResponse>({
      path: `/api/groups/${groupId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Modifie les informations d'un groupe existant. Seul l'administrateur propriétaire du groupe peut le modifier.
   *
   * @tags Groupes
   * @name UpdateGroup
   * @summary Modifier un groupe
   * @request PUT:/api/groups/{groupId}
   * @secure
   */
  updateGroup = (
    groupId: string,
    data: GroupRequest,
    params: RequestParams = {},
  ) =>
    this.request<GroupResponse, ErrorResponse>({
      path: `/api/groups/${groupId}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Crée une invitation pour rejoindre un groupe. Génère un token UUID unique et retourne un lien d'invitation. Seul l'administrateur du groupe peut créer des invitations. **Note** : L'envoi d'email n'est pas géré par cette API.
   *
   * @tags Invitations
   * @name Invite
   * @summary Créer une invitation
   * @request POST:/api/groups/{groupId}/invite
   * @secure
   */
  invite = (
    groupId: string,
    data: InvitationRequest,
    params: RequestParams = {},
  ) =>
    this.request<InvitationResponse, ErrorResponse>({
      path: `/api/groups/${groupId}/invite`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de toutes les invitations d'un groupe (acceptées et en attente). Seul l'administrateur du groupe peut consulter cette liste.
   *
   * @tags Invitations
   * @name GetInvitations
   * @summary Lister les invitations d'un groupe
   * @request GET:/api/groups/{groupId}/invitations
   * @secure
   */
  getInvitations = (groupId: string, params: RequestParams = {}) =>
    this.request<InvitationResponse[], ErrorResponse>({
      path: `/api/groups/${groupId}/invitations`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Accepte une invitation en utilisant le token UUID. **Actions effectuées** : - Vérifie que le token est valide et non expiré - Crée un utilisateur avec l'email de l'invitation (sans mot de passe) - Ajoute l'utilisateur au groupe - Marque l'invitation comme acceptée - Génère et retourne un token JWT pour l'utilisateur Le JWT contient l'ID utilisateur et l'ID du groupe comme autorité.
   *
   * @tags Invitations
   * @name Accept
   * @summary Accepter une invitation
   * @request GET:/api/invite/{token}
   */
  accept = (token: string, params: RequestParams = {}) =>
    this.request<InvitationResponse, ErrorResponse>({
      path: `/api/invite/${token}`,
      method: "GET",
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de tous les groupes auxquels l'utilisateur appartient. Accessible à tous les utilisateurs authentifiés (admin ou invité).
   *
   * @tags Utilisateurs
   * @name GetUserGroups
   * @summary Lister mes groupes
   * @request GET:/api/groups/me
   * @secure
   */
  getUserGroups = (params: RequestParams = {}) =>
    this.request<GroupResponse[], ErrorResponse>({
      path: `/api/groups/me`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de tous les utilisateurs membres d'un groupe. L'utilisateur doit être membre du groupe pour accéder à cette information. **Autorisation** : Nécessite l'autorité `GROUP_{groupId}` dans le JWT.
   *
   * @tags Utilisateurs
   * @name GetUsersByGroup
   * @summary Lister les membres d'un groupe
   * @request GET:/api/groups/{groupId}/users
   * @secure
   */
  getUsersByGroup = (groupId: string, params: RequestParams = {}) =>
    this.request<UserResponse[], ErrorResponse>({
      path: `/api/groups/${groupId}/users`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Permet à un utilisateur authentifié de modifier son avatar et/ou son pseudo. Les champs sont optionnels, seuls les champs fournis seront mis à jour.
   *
   * @tags Utilisateurs
   * @name UpdateUserAvatar
   * @summary Mettre à jour le profil de l'utilisateur
   * @request PUT:/api/users/me/avatar
   * @secure
   */
  updateUserAvatar = (data: UpdateAvatarRequest, params: RequestParams = {}) =>
    this.request<UserResponse, ErrorResponse>({
      path: `/api/users/me/avatar`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de tous les souhaits du groupe. Affiche qui a réservé chaque cadeau (visible par tous les membres). L'utilisateur doit être membre du groupe.
   *
   * @tags Souhaits
   * @name GetGroupWishes
   * @summary Lister tous les souhaits d'un groupe
   * @request GET:/api/groups/{groupId}/wishes
   * @secure
   */
  getGroupWishes = (groupId: string, params: RequestParams = {}) =>
    this.request<WishResponse[], ErrorResponse>({
      path: `/api/groups/${groupId}/wishes`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Ajoute un nouveau souhait à sa propre liste dans le groupe. L'utilisateur doit être membre du groupe.
   *
   * @tags Souhaits
   * @name AddWish
   * @summary Créer un souhait
   * @request POST:/api/groups/{groupId}/wishes
   * @secure
   */
  addWish = (groupId: string, data: WishRequest, params: RequestParams = {}) =>
    this.request<WishResponse, ErrorResponse>({
      path: `/api/groups/${groupId}/wishes`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste de tous les souhaits créés par l'utilisateur authentifié dans ce groupe.
   *
   * @tags Souhaits
   * @name GetMyWishes
   * @summary Lister mes souhaits
   * @request GET:/api/groups/{groupId}/wishes/me
   * @secure
   */
  getMyWishes = (groupId: string, params: RequestParams = {}) =>
    this.request<WishResponse[], ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/me`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Retourne la liste des souhaits d'un utilisateur spécifique dans le groupe. L'utilisateur authentifié et l'utilisateur cible doivent être membres du même groupe.
   *
   * @tags Souhaits
   * @name GetUserWishes
   * @summary Lister les souhaits d'un utilisateur
   * @request GET:/api/groups/{groupId}/wishes/users/{userId}
   * @secure
   */
  getUserWishes = (
    groupId: string,
    userId: string,
    params: RequestParams = {},
  ) =>
    this.request<WishResponse[], ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/users/${userId}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Supprime un souhait de sa propre liste. Seul le créateur du souhait peut le supprimer.
   *
   * @tags Souhaits
   * @name DeleteWish
   * @summary Supprimer un souhait
   * @request DELETE:/api/groups/{groupId}/wishes/{wishId}
   * @secure
   */
  deleteWish = (groupId: string, wishId: string, params: RequestParams = {}) =>
    this.request<void, ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/${wishId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Modifie un souhait existant. Seul le créateur du souhait peut le modifier. Le souhait ne doit pas être réservé pour être modifié.
   *
   * @tags Souhaits
   * @name UpdateWish
   * @summary Modifier un souhait
   * @request PUT:/api/groups/{groupId}/wishes/{wishId}
   * @secure
   */
  updateWish = (
    groupId: string,
    wishId: string,
    data: WishRequest,
    params: RequestParams = {},
  ) =>
    this.request<WishResponse, ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/${wishId}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Annule la réservation d'un cadeau. Seul l'utilisateur qui a réservé le cadeau peut annuler sa réservation.
   *
   * @tags Souhaits
   * @name UnreserveWish
   * @summary Annuler une réservation
   * @request DELETE:/api/groups/{groupId}/wishes/{wishId}/reserve
   * @secure
   */
  unreserveWish = (
    groupId: string,
    wishId: string,
    params: RequestParams = {},
  ) =>
    this.request<WishResponse, ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/${wishId}/reserve`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Réserve un cadeau pour un autre membre du groupe. **Règles** : - Un utilisateur ne peut pas réserver ses propres souhaits - Un souhait déjà réservé ne peut pas être réservé à nouveau - L'utilisateur doit être membre du groupe
   *
   * @tags Souhaits
   * @name ReserveWish
   * @summary Réserver un souhait
   * @request POST:/api/groups/{groupId}/wishes/{wishId}/reserve
   * @secure
   */
  reserveWish = (groupId: string, wishId: string, params: RequestParams = {}) =>
    this.request<WishResponse, ErrorResponse>({
      path: `/api/groups/${groupId}/wishes/${wishId}/reserve`,
      method: "POST",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Extrait les métadonnées d'une URL de produit (titre, description, image, prix). Utilise les tags OpenGraph, meta tags, et analyse le contenu HTML. **Utilisation** : Permet de pré-remplir automatiquement un formulaire d'ajout de souhait en analysant la page web du produit souhaité.
   *
   * @tags Metadata
   * @name ExtractMetadata
   * @summary Extraire les métadonnées d'une URL
   * @request GET:/api/metadata
   * @secure
   */
  extractMetadata = (
    query: {
      /**
       * URL de la page à analyser
       * @format uri
       * @example "https://www.amazon.fr/produit-exemple"
       */
      url: string;
    },
    params: RequestParams = {},
  ) =>
    this.request<MetadataResponse, ErrorResponse>({
      path: `/api/metadata`,
      method: "GET",
      query: query,
      secure: true,
      format: "json",
      ...params,
    });
}
