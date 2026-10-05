ALTER TABLE `post`
ADD COLUMN `category_id` BIGINT NULL AFTER `content_md`;

CREATE TABLE
    `category` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `name` VARCHAR(100) NOT NULL,
        `slug` VARCHAR(100) NOT NULL,
        `parent_id` BIGINT NULL,
        `sort` INT NOT NULL DEFAULT 0,
        `is_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
        `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_category_slug` (`slug`),
        KEY `idx_category_parent` (`parent_id`),
        KEY `idx_category_enabled` (`is_enabled`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO
    `category` (`name`, `slug`, `parent_id`, `sort`, `is_enabled`)
VALUES
    ('其它', 'other', NULL, 99, TRUE),
    ('Java', 'java', NULL, 1, TRUE),
    ('C++', 'cpp', NULL, 2, TRUE),
    ('计算机基础', 'computer-foundation', NULL, 3, TRUE),
    ('Java 基础', 'java-basic', 1, 1, TRUE),
    ('C++ 基础', 'cpp-basic', 2, 1, TRUE),
    ('数据结构', 'data-structure', 3, 1, TRUE),
    ('操作系统', 'operating-system', 3, 2, TRUE),
    ('计算机网络', 'computer-network', 3, 3, TRUE);