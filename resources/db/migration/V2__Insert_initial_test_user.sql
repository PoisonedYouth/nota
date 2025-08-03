-- Insert initial test user (password: 'password')
INSERT INTO users (username, password, must_change_password, created_at, updated_at)
VALUES ('testuser', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
