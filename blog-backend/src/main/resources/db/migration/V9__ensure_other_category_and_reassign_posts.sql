INSERT INTO
    `category` (
        `name`,
        `slug`,
        `parent_id`,
        `sort`,
        `is_enabled`,
        `created_at`,
        `updated_at`
    )
SELECT
    '其它',
    'other',
    NULL,
    99,
    TRUE,
    NOW (),
    NOW ()
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            `category`
        WHERE
            `slug` = 'other'
            AND `is_enabled` = TRUE
    );

UPDATE `post` p
SET
    p.`category_id` = (
        SELECT
            c.`id`
        FROM
            `category` c
        WHERE
            c.`slug` = 'other'
        LIMIT
            1
    )
WHERE
    p.`category_id` IS NULL
    OR p.`category_id` NOT IN (
        SELECT
            c2.`id`
        FROM
            `category` c2
    );