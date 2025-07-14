-- Insert initial test user
INSERT INTO users (username, password, created_at, updated_at)
VALUES ('testuser', 'password', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
