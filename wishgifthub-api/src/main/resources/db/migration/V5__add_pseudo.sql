-- Migration pour ajouter la colonne pseudo aux tables users et invitations

ALTER TABLE users ADD COLUMN IF NOT EXISTS pseudo VARCHAR(100);
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS pseudo VARCHAR(100);

