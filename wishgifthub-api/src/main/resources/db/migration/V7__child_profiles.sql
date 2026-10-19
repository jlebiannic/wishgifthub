ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ADD COLUMN parent_id UUID REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_users_parent_id ON users(parent_id);
ALTER TABLE users ADD CONSTRAINT child_profile_credentials CHECK (
    (parent_id IS NULL AND email IS NOT NULL)
    OR (parent_id IS NOT NULL AND parent_id <> id AND email IS NULL
        AND password_hash IS NULL AND is_admin = FALSE)
);
