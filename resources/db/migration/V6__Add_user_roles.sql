-- Add role column to users table
ALTER TABLE users ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';

-- Set testuser as admin
UPDATE users SET role = 'ADMIN' WHERE username = 'testuser';
