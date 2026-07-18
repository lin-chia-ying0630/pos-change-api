INSERT INTO code_description (code_group, code_field, code_before, code_after, code_description)
VALUES
    ('CHT-code', 'policyNo', '保單號碼', NULL, '保單號碼'),
    ('CHT-code', 'policySeq', '保單序號', NULL, '保單序號'),
    ('CHT-code', 'addressType', '地址類型', NULL, '地址類型'),
    ('CHT-code', 'zipCode3', '郵遞區號前三碼', NULL, '郵遞區號前三碼'),
    ('CHT-code', 'zipCode2', '郵遞區號後三碼', NULL, '郵遞區號後三碼'),
    ('CHT-code', 'fullWidthAddress', '中文地址', NULL, '中文地址'),
    ('CHT-code', 'halfWidthAddress', '電子郵件／電話／手機', NULL, '電子郵件／電話／手機'),
    ('CHT-code', 'mainProductCode', '主約險種代碼', NULL, '主約險種代碼'),
    ('CHT-code', 'mainPolicyYears', '主約年期', NULL, '主約年期'),
    ('CHT-code', 'rideType', '主附約類型', NULL, '主附約類型'),
    ('CHT-code', 'rideOrder', '主附約序號', NULL, '主附約序號'),
    ('CHT-code', 'productCode', '險種代碼', NULL, '險種代碼'),
    ('CHT-code', 'policyYears', '年期', NULL, '年期'),
    ('CHT-code', 'insuredAmount', '保額', NULL, '保額'),
    ('CHT-code', 'premium', '保費', NULL, '保費'),
    ('CHT-code', 'createdAt', '建立時間', NULL, '建立時間'),
    ('CHT-code', 'updatedAt', '更新時間', NULL, '更新時間') AS incoming
ON DUPLICATE KEY UPDATE
    code_after = incoming.code_after,
    code_description = incoming.code_description;
