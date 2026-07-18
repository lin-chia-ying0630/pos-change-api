-- 002 改以 main_policy_ride 的主約列為唯一保額來源，先轉換既有待覆核草稿。
INSERT IGNORE INTO policy_change_field (
    policy_no,
    policy_seq,
    change_case_no,
    change_item,
    change_field,
    change_key,
    content_before,
    content_after
)
SELECT policy_no,
       policy_seq,
       change_case_no,
       change_item,
       'main_policy_ride.000.insured_amount',
       '000',
       content_before,
       content_after
FROM policy_change_field
WHERE change_field = 'main_policy_master.insured_amount';

DELETE FROM policy_change_field
WHERE change_field IN (
    'main_policy_master.insured_amount',
    'main_product_code',
    'main_policy_years',
    'insured_amount'
);

DELETE FROM code_description
WHERE code_group = 'CHT-code'
  AND code_field IN ('mainProductCode', 'mainPolicyYears');

ALTER TABLE main_policy_master
    DROP COLUMN main_product_code,
    DROP COLUMN main_policy_years,
    DROP COLUMN insured_amount;
