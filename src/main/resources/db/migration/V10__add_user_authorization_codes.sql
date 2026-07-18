INSERT INTO code_description (code_group, code_field, code_before, code_after, code_description)
VALUES
    ('main-user', 'authorities', 'CREATE', 'user', '新增'),
    ('main-user', 'authorities', 'UPDATE', 'user', '修改'),
    ('main-user', 'authorities', 'DELETE', 'user', '刪除'),
    ('main-user', 'authorities', 'REVIEW', 'admin', '覆核')
ON DUPLICATE KEY UPDATE
    code_after = VALUES(code_after),
    code_description = VALUES(code_description);
