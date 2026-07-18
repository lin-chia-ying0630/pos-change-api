INSERT INTO code_description (code_group, code_field, code_before, code_after, code_description)
VALUES
    ('main-screen', 'screen', 'CREATE', 'MAKER', '新增'),
    ('main-screen', 'screen', 'UPDATE', 'MAKER', '修改'),
    ('main-screen', 'screen', 'DELETE', 'MAKER', '刪除'),
    ('main-screen', 'screen', 'REVIEW', 'REVIEWER', '覆核') AS incoming
ON DUPLICATE KEY UPDATE
    code_after = incoming.code_after,
    code_description = incoming.code_description;
