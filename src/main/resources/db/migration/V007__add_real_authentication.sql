-- V007__add_real_authentication.sql
-- Backs real username/password login. AuthController previously issued a JWT
-- for any userId with no credential check at all, which was fine for load
-- testing but not for anything else.
--
-- Roles live in a separate join table rather than a Postgres array column so
-- the schema matches Hibernate's @ElementCollection mapping on User and stays
-- identical under the H2 profile used in tests.
-- Created: MES 5 (Authentication)

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL UNIQUE,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One row per role granted to a user.
CREATE TABLE user_roles (
  owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL
);

-- Create indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_user_roles_owner ON user_roles(owner_id);
-- Add comment
COMMENT ON TABLE users IS 'Application users authenticated via username/password login';
COMMENT ON TABLE user_roles IS 'Roles granted to each user, one row per role';
