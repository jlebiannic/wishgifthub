ALTER TABLE wishes ADD COLUMN collective BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE wishes ADD COLUMN max_participants INTEGER NOT NULL DEFAULT 1;
ALTER TABLE wishes ADD CONSTRAINT chk_wish_capacity CHECK (
    (collective = FALSE AND max_participants = 1)
    OR (collective = TRUE AND max_participants >= 2)
);

CREATE TABLE wish_participants (
    wish_id UUID NOT NULL REFERENCES wishes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (wish_id, user_id)
);
CREATE INDEX idx_wish_participants_user ON wish_participants(user_id);
