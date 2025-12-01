-- Migration pour augmenter la taille des colonnes URL et description

-- Augmenter la taille de la colonne url (255 -> 2048)
ALTER TABLE wishes ALTER COLUMN url TYPE VARCHAR(2048);

-- Augmenter la taille de la colonne image_url (255 -> 2048)
ALTER TABLE wishes ALTER COLUMN image_url TYPE VARCHAR(2048);

-- Augmenter la taille de la colonne description (255 -> 10000)
ALTER TABLE wishes ALTER COLUMN description TYPE VARCHAR(10000);

-- Augmenter la taille de la colonne price (255 -> 100)
ALTER TABLE wishes ALTER COLUMN price TYPE VARCHAR(100);

-- Commentaires
COMMENT ON COLUMN wishes.url IS 'URL du produit (max 2048 caractères)';
COMMENT ON COLUMN wishes.image_url IS 'URL de l''image du produit (max 2048 caractères)';
COMMENT ON COLUMN wishes.description IS 'Description détaillée du souhait (max 2000 caractères)';

