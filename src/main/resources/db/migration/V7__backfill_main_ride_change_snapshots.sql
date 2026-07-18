-- 為既有 002 案件補上主約完整資料列快照，讓查詢與覆核畫面呈現完整內容。
INSERT IGNORE INTO policy_change_file (
    policy_no,
    policy_seq,
    change_case_no,
    change_item,
    change_file,
    change_key,
    content_before,
    content_after
)
SELECT field.policy_no,
       field.policy_seq,
       field.change_case_no,
       field.change_item,
       'main_policy_ride',
       ride.ride_order,
       JSON_OBJECT(
           'policyNo', ride.policy_no,
           'policySeq', ride.policy_seq,
           'rideType', ride.ride_type,
           'rideOrder', ride.ride_order,
           'productCode', ride.product_code,
           'policyYears', ride.policy_years,
           'insuredAmount', CAST(field.content_before AS DECIMAL(10, 2)),
           'premium', ride.premium
       ),
       JSON_OBJECT(
           'policyNo', ride.policy_no,
           'policySeq', ride.policy_seq,
           'rideType', ride.ride_type,
           'rideOrder', ride.ride_order,
           'productCode', ride.product_code,
           'policyYears', ride.policy_years,
           'insuredAmount', CAST(field.content_after AS DECIMAL(10, 2)),
           'premium', ride.premium
       )
FROM policy_change_field field
JOIN main_policy_ride ride
  ON ride.policy_no = field.policy_no
 AND ride.policy_seq = field.policy_seq
 AND ride.ride_order = field.change_key
WHERE field.change_item = '002'
  AND field.change_field = 'main_policy_ride.000.insured_amount'
  AND field.change_key = '000';
