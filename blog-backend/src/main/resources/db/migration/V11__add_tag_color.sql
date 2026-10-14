SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tag'
      AND column_name = 'color'
);

SET @stmt := IF(
    @column_exists = 0,
    'ALTER TABLE `tag` ADD COLUMN `color` VARCHAR(32) NOT NULL DEFAULT ''#4098FC'' AFTER `name`',
    'SELECT 1'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @updated_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tag'
      AND column_name = 'updated_at'
);

SET @stmt := IF(
    @updated_at_exists = 0,
    'ALTER TABLE `tag` ADD COLUMN `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`',
    'SELECT 1'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `tag`
SET `color` = '#4098FC'
WHERE `color` IS NULL OR `color` = '';