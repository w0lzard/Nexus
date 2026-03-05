-- Add missing index on likes(user_id) for efficient per-user like lookups
-- (idx_likes_post already exists from V2; this covers the other direction)
CREATE INDEX IF NOT EXISTS idx_likes_user ON likes(user_id);

