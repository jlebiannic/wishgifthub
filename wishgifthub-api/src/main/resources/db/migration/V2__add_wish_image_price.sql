-- Migration pour ajouter les colonnes image_url et price à la table wishes

-- Ajouter la colonne image_url
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);

-- Ajouter la colonne price
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS price VARCHAR(50);

-- Commentaires
COMMENT ON COLUMN wishes.image_url IS 'URL de l''image du produit souhaité';
COMMENT ON COLUMN wishes.price IS 'Prix estimé du produit (format texte pour gérer différentes devises)';

