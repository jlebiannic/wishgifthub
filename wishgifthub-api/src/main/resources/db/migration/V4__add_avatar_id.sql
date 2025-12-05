-- Ajouter la colonne avatar_id à la table users
ALTER TABLE users ADD COLUMN avatar_id VARCHAR(50);

-- Ajouter la colonne avatar_id à la table invitations
ALTER TABLE invitations ADD COLUMN avatar_id VARCHAR(50);

