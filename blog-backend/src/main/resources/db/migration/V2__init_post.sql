CREATE TABLE
    IF NOT EXISTS `post` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `title` VARCHAR(255) NOT NULL,
        `slug` VARCHAR(255) NOT NULL,
        `content_md` MEDIUMTEXT NOT NULL,
        `content_html` MEDIUMTEXT NOT NULL,
        `toc_json` JSON NULL,
        `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
        `published_at` DATETIME NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        `deleted_at` DATETIME NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_slug` (`slug`),
        KEY `idx_status_published` (`status`, `published_at` DESC),
        FULLTEXT KEY `ft_title_content` (`title`, `content_md`)
        WITH
            PARSER ngram
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;