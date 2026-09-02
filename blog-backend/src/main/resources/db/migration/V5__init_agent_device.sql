CREATE TABLE
    IF NOT EXISTS `agent_device` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `device_id` VARCHAR(64) NOT NULL,
        `user_id` BIGINT NOT NULL,
        `name` VARCHAR(128) NULL,
        `os` VARCHAR(16) NULL,
        `arch` VARCHAR(16) NULL,
        `agent_version` VARCHAR(32) NULL,
        `jwt_hash` VARCHAR(255) NOT NULL,
        `capabilities` JSON NULL,
        `status` VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
        `last_seen_at` DATETIME NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_device_id` (`device_id`),
        KEY `idx_user` (`user_id`, `status`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;