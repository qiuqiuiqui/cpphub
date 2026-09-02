CREATE TABLE
    IF NOT EXISTS `user` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `username` VARCHAR(64) NOT NULL,
        `password_hash` VARCHAR(255) NOT NULL,
        `role` VARCHAR(16) NOT NULL DEFAULT 'USER',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_username` (`username`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Initial admin account (bcrypt of 'admin123', cost=10)
-- BCrypt hash generated separately; placeholder below. Use DataInitializer at app boot
-- or run this INSERT manually if you prefer:
-- INSERT INTO `user` (username, password_hash, role) VALUES ('admin', '$2a$10$...', 'ADMIN');