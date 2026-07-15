CREATE TABLE ${session_schema}.auth_users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('VIEWER', 'ADMIN')),
    enabled BOOLEAN NOT NULL
);

INSERT INTO ${session_schema}.auth_users (id, username, password_hash, role, enabled)
VALUES
    (1, 'viewer', '$2a$12$ZeFOjiE.MkSC3hwCbx20AOuEdGnJKcHJInZJkdlmksPvsiCcz3JSC', 'VIEWER', TRUE),
    (2, 'admin', '$2a$12$1Ws9gK9NCAGka8qgdIYcmupjBuSlKsrioDngAUdXxQFgZo8nCZyXG', 'ADMIN', TRUE);
