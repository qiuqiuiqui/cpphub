ALTER TABLE category
ADD COLUMN depth TINYINT NOT NULL DEFAULT 1 COMMENT '分类深度：1 一级，2 二级，3 三级';

UPDATE category c
LEFT JOIN category p ON p.id = c.parent_id
SET
    c.depth = CASE
        WHEN c.parent_id IS NULL THEN 1
        WHEN p.depth IS NULL THEN 2
        WHEN p.depth >= 3 THEN 3
        ELSE p.depth + 1
    END
WHERE
    c.depth IS NULL
    OR c.depth = 1;