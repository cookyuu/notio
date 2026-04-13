-- Fix the default test user's password hash.
-- Plain password: password123
UPDATE users
SET password = '$2b$10$Fur7nlI4LFlasP0ztEFaPemhZQDf2lBr0HEnyuHn57R9uA.cBsWLK',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'user@example.com'
  AND deleted_at IS NULL;
