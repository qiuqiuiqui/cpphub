CREATE TABLE
    IF NOT EXISTS `agent_pairing_code` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `code` VARCHAR(16) NOT NULL,
        `user_id` BIGINT NOT NULL,
        `device_name` VARCHAR(128) NULL,
        `expires_at` DATETIME NOT NULL,
        `used_at` DATETIME NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_code` (`code`),
        KEY `idx_user_unused` (`user_id`, `used_at`, `expires_at`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE
    IF NOT EXISTS `agent_command_log` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `request_id` VARCHAR(64) NOT NULL,
        `device_id` VARCHAR(64) NOT NULL,
        `user_id` BIGINT NULL,
        `command` VARCHAR(64) NOT NULL,
        `params_json` TEXT NULL,
        `exit_code` INT NULL,
        `duration_ms` BIGINT NULL,
        `status` VARCHAR(16) NOT NULL,
        `error_msg` TEXT NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        KEY `idx_device_ts` (`device_id`, `created_at` DESC),
        KEY `idx_status_ts` (`status`, `created_at` DESC)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;