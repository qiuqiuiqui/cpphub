ALTER TABLE `post`
    ADD COLUMN `word_count` INT NOT NULL DEFAULT 0 AFTER `content_md`,
    ADD COLUMN `view_count` INT NOT NULL DEFAULT 0 AFTER `word_count`;

UPDATE `post`
SET `word_count` = CASE WHEN `content_md` IS NULL THEN 0 ELSE CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`content_md`, '\r', ' '), '\n', ' '), '\t', ' '), '  ', ' '), '  ', ' '), '  ', ' '), '  ', ' '), '  ', ' '), '  ', ' '), '  ', ' ')) END,
    `view_count` = 0
WHERE `word_count` = 0 OR `view_count` IS NULL;
