INSERT INTO main_policy_master (policy_no, policy_seq, premium)
VALUES ('P000000001', 1, 15925.9089) AS incoming
ON DUPLICATE KEY UPDATE
    premium = incoming.premium;

INSERT INTO main_policy_address (policy_no, policy_seq, address_type, zip_code3, zip_code2, full_width_address, half_width_address)
VALUES
    ('P000000001', 1, '01', '100', '001', '臺北市中正區重慶南路一段１號', 'No.1, Sec.1, Chongqing S. Rd., Zhongzheng Dist., Taipei City'),
    ('P000000001', 1, '02', '104', '001', '臺北市中山區南京東路二段１００號', 'No.100, Sec.2, Nanjing E. Rd., Zhongshan Dist., Taipei City'),
    ('P000000001', 1, '31', NULL, NULL, 'policyholder@example.com', 'policyholder@example.com') AS incoming
ON DUPLICATE KEY UPDATE
    zip_code3 = incoming.zip_code3,
    zip_code2 = incoming.zip_code2,
    full_width_address = incoming.full_width_address,
    half_width_address = incoming.half_width_address;

INSERT INTO main_policy_ride (policy_no, policy_seq, ride_type, ride_order, product_code, policy_years, insured_amount, premium)
VALUES
    ('P000000001', 1, '1', '000', 'LIFE', 20, 1000000.00, 12345.6789),
    ('P000000001', 1, '2', '001', 'ADDR', 20, 500000.00, 2345.6700),
    ('P000000001', 1, '3', '002', 'FAMI', 20, 300000.00, 1234.5600) AS incoming
ON DUPLICATE KEY UPDATE
    ride_type = incoming.ride_type,
    product_code = incoming.product_code,
    policy_years = incoming.policy_years,
    insured_amount = incoming.insured_amount,
    premium = incoming.premium;

UPDATE main_policy_master master
SET premium = (
    SELECT COALESCE(SUM(ride.premium), 0)
    FROM main_policy_ride ride
    WHERE ride.policy_no = master.policy_no
      AND ride.policy_seq = master.policy_seq
)
WHERE policy_no = 'P000000001'
  AND policy_seq = 1;
