CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(128) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (username)
);

CREATE TABLE IF NOT EXISTS authorities (
    username VARCHAR(128) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username),
    UNIQUE KEY uk_authorities_username_authority (username, authority)
);
