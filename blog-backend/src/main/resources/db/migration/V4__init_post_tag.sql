CREATE TABLE
    IF NOT EXISTS `post_tag` (
        `post_id` BIGINT NOT NULL,
        `tag_id` BIGINT NOT NULL,
        PRIMARY KEY (`post_id`, `tag_id`),
        KEY `idx_tag_post` (`tag_id`, `post_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;