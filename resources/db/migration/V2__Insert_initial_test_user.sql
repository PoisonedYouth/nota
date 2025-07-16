-- Insert initial test user
INSERT INTO users (username, password, must_change_password, created_at, updated_at)
VALUES ('testuser', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
