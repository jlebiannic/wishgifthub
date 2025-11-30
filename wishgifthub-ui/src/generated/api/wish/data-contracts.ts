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

/** @example {"password":"SecureP@ssw0rd","email":"admin@example.com"} */
export interface AuthRequest {
  /**
   * Adresse email de l'utilisateur
   * @format email
   * @example "admin@example.com"
   */
  email: string;
  /**
   * Mot de passe (minimum 8 caractères)
   * @format password
   * @minLength 8
   * @example "SecureP@ssw0rd"
   */
  password: string;
}

/** @example {"isAdmin":true,"userId":"123e4567-e89b-12d3-a456-426614174000","token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjNlNDU2Ny1lODliLTEyZDMtYTQ1Ni00MjY2MTQxNzQwMDAiLCJpc0FkbWluIjp0cnVlLCJncm91cElkcyI6W10sImlhdCI6MTYzMTcwMDAwMCwiZXhwIjoxNjMxNzg2NDAwfQ.signature"} */
export interface AuthResponse {
  /**
   * Identifiant unique de l'utilisateur
   * @format uuid
   * @example "123e4567-e89b-12d3-a456-426614174000"
   */
  userId: string;
  /**
   * Indique si l'utilisateur est administrateur
   * @example true
   */
  isAdmin: boolean;
  /**
   * Token JWT pour l'authentification
   * @example "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjNlNDU2Ny1lODliLTEyZDMtYTQ1Ni00MjY2MTQxNzQwMDAiLCJpc0FkbWluIjp0cnVlLCJncm91cElkcyI6W10sImlhdCI6MTYzMTcwMDAwMCwiZXhwIjoxNjMxNzg2NDAwfQ.signature"
   */
  token: string;
}

export interface ErrorResponse {
  /**
   * Message d'erreur détaillé
   * @example "Une erreur s'est produite"
   */
  message: string;
  /**
   * Horodatage de l'erreur
   * @format date-time
   * @example "2025-11-10T17:30:00Z"
   */
  timestamp: string;
}

/** @example {"createdAt":"2025-11-10T10:00:00Z","name":"Noël en famille 2025","adminId":"987e6543-e21b-12d3-a456-426614174999","jwtToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","id":"123e4567-e89b-12d3-a456-426614174000","type":"noël"} */
export interface GroupResponse {
  /**
   * Identifiant unique du groupe
   * @format uuid
   * @example "123e4567-e89b-12d3-a456-426614174000"
   */
  id: string;
  /**
   * Nom du groupe
   * @example "Noël en famille 2025"
   */
  name: string;
  /**
   * Type d'événement
   * @example "noël"
   */
  type: string;
  /**
   * Identifiant de l'administrateur du groupe
   * @format uuid
   * @example "987e6543-e21b-12d3-a456-426614174999"
   */
  adminId: string;
  /**
   * Date de création du groupe
   * @format date-time
   * @example "2025-11-10T10:00:00Z"
   */
  createdAt: string;
  /**
   * Token JWT mis à jour avec l'ID du groupe (présent uniquement lors de la création)
   * @example "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   */
  jwtToken?: string | null;
}

/** @example {"name":"Noël en famille 2025","type":"noël"} */
export interface GroupRequest {
  /**
   * Nom du groupe
   * @minLength 1
   * @maxLength 255
   * @example "Noël en famille 2025"
   */
  name: string;
  /**
   * Type d'événement (actuellement seul 'noël' est supporté)
   * @example "noël"
   */
  type: "noël";
}

/** @example {"email":"user@example.com"} */
export interface InvitationRequest {
  /**
   * Adresse email de la personne à inviter
   * @format email
   * @example "user@example.com"
   */
  email: string;
}

/** @example {"createdAt":"2025-11-10T10:00:00Z","groupId":"987e6543-e21b-12d3-a456-426614174999","accepted":false,"jwtToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","id":"123e4567-e89b-12d3-a456-426614174000","email":"user@example.com","token":"046b6c7f-0b8a-43b9-b35d-6489e6daee91","invitationLink":"http://localhost:8080/api/invite/abc12345-def6-7890-ghij-klmnopqrstuv"} */
export interface InvitationResponse {
  /**
   * Identifiant unique de l'invitation
   * @format uuid
   * @example "123e4567-e89b-12d3-a456-426614174000"
   */
  id: string;
  /**
   * Email de la personne invitée
   * @format email
   * @example "user@example.com"
   */
  email: string;
  /**
   * Identifiant du groupe
   * @format uuid
   * @example "987e6543-e21b-12d3-a456-426614174999"
   */
  groupId: string;
  /**
   * Token UUID pour accepter l'invitation
   * @format uuid
   */
  token: string;
  /**
   * Indique si l'invitation a été acceptée
   * @example false
   */
  accepted: boolean;
  /**
   * Date de création de l'invitation
   * @format date-time
   * @example "2025-11-10T10:00:00Z"
   */
  createdAt: string;
  /**
   * Lien complet pour accepter l'invitation
   * @format uri
   * @example "http://localhost:8080/api/invite/abc12345-def6-7890-ghij-klmnopqrstuv"
   */
  invitationLink?: string | null;
  /**
   * Token JWT généré après acceptation de l'invitation
   * @example "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   */
  jwtToken?: string | null;
}

/** @example {"createdAt":"2025-11-01T10:00:00Z","id":"987e6543-e21b-12d3-a456-426614174999","isAdmin":true,"email":"admin@example.com"} */
export interface UserResponse {
  /**
   * Identifiant unique de l'utilisateur
   * @format uuid
   * @example "987e6543-e21b-12d3-a456-426614174999"
   */
  id: string;
  /**
   * Adresse email de l'utilisateur
   * @format email
   * @example "admin@example.com"
   */
  email: string;
  /**
   * Indique si l'utilisateur est administrateur
   * @example true
   */
  isAdmin?: boolean;
  /**
   * Date de création du compte
   * @format date-time
   * @example "2025-11-01T10:00:00Z"
   */
  createdAt: string;
}

/** @example {"createdAt":"2025-11-10T10:00:00Z","giftName":"Livre de cuisine","groupId":"123e4567-e89b-12d3-a456-426614174000","reservedBy":"456e7890-e12b-12d3-a456-426614174111","description":"Livre de recettes italiennes avec photos","id":"111e2222-e33b-44d5-a666-777888999000","userId":"987e6543-e21b-12d3-a456-426614174999","url":"https://example.com/livre"} */
export interface WishResponse {
  /**
   * Identifiant unique du souhait
   * @format uuid
   * @example "111e2222-e33b-44d5-a666-777888999000"
   */
  id: string;
  /**
   * Identifiant de l'utilisateur qui a créé le souhait
   * @format uuid
   * @example "987e6543-e21b-12d3-a456-426614174999"
   */
  userId: string;
  /**
   * Identifiant du groupe
   * @format uuid
   * @example "123e4567-e89b-12d3-a456-426614174000"
   */
  groupId: string;
  /**
   * Nom du cadeau souhaité
   * @example "Livre de cuisine"
   */
  giftName: string;
  /**
   * Description détaillée du cadeau
   * @example "Livre de recettes italiennes avec photos"
   */
  description?: string | null;
  /**
   * Lien vers le produit
   * @format uri
   * @example "https://example.com/livre"
   */
  url?: string | null;
  /**
   * Identifiant de l'utilisateur qui a réservé le cadeau (null si non réservé)
   * @format uuid
   * @example "456e7890-e12b-12d3-a456-426614174111"
   */
  reservedBy?: string | null;
  /**
   * Date de création du souhait
   * @format date-time
   * @example "2025-11-10T10:00:00Z"
   */
  createdAt: string;
}

/** @example {"giftName":"Livre de cuisine","description":"Livre de recettes italiennes avec photos","url":"https://example.com/livre"} */
export interface WishRequest {
  /**
   * Nom du cadeau souhaité
   * @minLength 1
   * @maxLength 255
   * @example "Livre de cuisine"
   */
  giftName: string;
  /**
   * Description détaillée du cadeau (optionnel)
   * @maxLength 1000
   * @example "Livre de recettes italiennes avec photos"
   */
  description?: string | null;
  /**
   * Lien vers le produit (optionnel)
   * @format uri
   * @example "https://example.com/livre"
   */
  url?: string | null;
}
