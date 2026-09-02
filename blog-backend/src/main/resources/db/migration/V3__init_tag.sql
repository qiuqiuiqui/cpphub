CREATE TABLE
    IF NOT EXISTS `tag` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `name` VARCHAR(64) NOT NULL,
        `use_count` INT NOT NULL DEFAULT 0,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_name` (`name`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;