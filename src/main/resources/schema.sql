CREATE DATABASE IF NOT EXISTS `main`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `main`;

CREATE TABLE IF NOT EXISTS main_policy_master (
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    main_product_code VARCHAR(4) NOT NULL,
    main_policy_years INT(2) NOT NULL,
    insured_amount DECIMAL(10, 2) NOT NULL,
    premium DECIMAL(17, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_no, policy_seq)
);

CREATE TABLE IF NOT EXISTS main_policy_address (
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    address_type VARCHAR(2) NOT NULL,
    zip_code3 VARCHAR(3),
    zip_code2 VARCHAR(2),
    full_width_address VARCHAR(255),
    half_width_address VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_no, policy_seq, address_type),
    CONSTRAINT fk_policy_address_master
        FOREIGN KEY (policy_no, policy_seq)
        REFERENCES main_policy_master (policy_no, policy_seq)
);

CREATE TABLE IF NOT EXISTS main_policy_ride (
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    ride_type VARCHAR(1) NOT NULL,
    ride_order VARCHAR(3) NOT NULL,
    product_code VARCHAR(4) NOT NULL,
    policy_years INT(2) NOT NULL,
    insured_amount DECIMAL(10, 2) NOT NULL,
    premium DECIMAL(17, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_no, policy_seq, ride_order),
    CONSTRAINT fk_policy_ride_master
        FOREIGN KEY (policy_no, policy_seq)
        REFERENCES main_policy_master (policy_no, policy_seq)
);

CREATE TABLE IF NOT EXISTS policy_change_acceptance (
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    change_case_no VARCHAR(20) NOT NULL,
    acceptance_status VARCHAR(1) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_no, policy_seq, change_case_no),
    CONSTRAINT fk_change_acceptance_master
        FOREIGN KEY (policy_no, policy_seq)
        REFERENCES main_policy_master (policy_no, policy_seq)
);

CREATE TABLE IF NOT EXISTS policy_change_item (
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    change_case_no VARCHAR(20) NOT NULL,
    change_item VARCHAR(3) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_no, policy_seq, change_case_no, change_item),
    CONSTRAINT fk_change_item_acceptance
        FOREIGN KEY (policy_no, policy_seq, change_case_no)
        REFERENCES policy_change_acceptance (policy_no, policy_seq, change_case_no)
);

CREATE TABLE IF NOT EXISTS policy_change_field (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    change_case_no VARCHAR(20) NOT NULL,
    change_item VARCHAR(3) NOT NULL,
    change_field VARCHAR(64) NOT NULL,
    change_key VARCHAR(128),
    content_before TEXT,
    content_after TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_change_field_item
        FOREIGN KEY (policy_no, policy_seq, change_case_no, change_item)
        REFERENCES policy_change_item (policy_no, policy_seq, change_case_no, change_item)
);

CREATE TABLE IF NOT EXISTS policy_change_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT(3) NOT NULL,
    change_case_no VARCHAR(20) NOT NULL,
    change_item VARCHAR(3) NOT NULL,
    change_file VARCHAR(64) NOT NULL,
    content_before TEXT,
    content_after TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_change_file_item
        FOREIGN KEY (policy_no, policy_seq, change_case_no, change_item)
        REFERENCES policy_change_item (policy_no, policy_seq, change_case_no, change_item)
);

CREATE TABLE IF NOT EXISTS code_description (
    code_group VARCHAR(64) NOT NULL,
    code_field VARCHAR(64) NOT NULL,
    code_before VARCHAR(64) NOT NULL,
    code_after VARCHAR(64),
    code_description VARCHAR(255) NOT NULL,
    PRIMARY KEY (code_group, code_field, code_before)
);

INSERT INTO code_description (code_group, code_field, code_before, code_after, code_description)
VALUES
    ('main-policy-address', 'address_type', '01', NULL, '通訊地址'),
    ('main-policy-address', 'address_type', '02', NULL, '戶籍地址'),
    ('main-policy-address', 'address_type', '11', NULL, '通訊電話'),
    ('main-policy-address', 'address_type', '12', NULL, '戶籍電話'),
    ('main-policy-address', 'address_type', '31', NULL, 'email'),
    ('main-policy-ride', 'ride_type', '1', NULL, '主約'),
    ('main-policy-ride', 'ride_type', '2', NULL, '本人附約'),
    ('main-policy-ride', 'ride_type', '3', NULL, '眷屬附約'),
    ('policy-change-acceptance', 'acceptance_status', 'P', NULL, '受理中'),
    ('policy-change-acceptance', 'acceptance_status', 'A', NULL, '處理中'),
    ('policy-change-acceptance', 'acceptance_status', 'S', NULL, '完成'),
    ('policy-change-acceptance', 'acceptance_status', 'C', NULL, '取消'),
    ('policy-change-item', 'change_item', '001', NULL, '地址變更'),
    ('policy-change-item', 'change_item', '002', NULL, '主保額變更'),
    ('policy-change-item', 'change_item', '003', NULL, '附約保額變更')
ON DUPLICATE KEY UPDATE
    code_after = VALUES(code_after),
    code_description = VALUES(code_description);

INSERT INTO main_policy_master (policy_no, policy_seq, main_product_code, main_policy_years, insured_amount, premium)
VALUES ('P000000001', 1, 'LIFE', 20, 1000000.00, 12345.6789)
ON DUPLICATE KEY UPDATE
    main_product_code = VALUES(main_product_code),
    main_policy_years = VALUES(main_policy_years),
    insured_amount = VALUES(insured_amount),
    premium = VALUES(premium);

INSERT INTO main_policy_address (policy_no, policy_seq, address_type, zip_code3, zip_code2, full_width_address, half_width_address)
VALUES ('P000000001', 1, '01', '100', '01', '臺北市中正區重慶南路一段１號', 'No.1, Sec.1, Chongqing S. Rd., Zhongzheng Dist., Taipei City')
ON DUPLICATE KEY UPDATE
    zip_code3 = VALUES(zip_code3),
    zip_code2 = VALUES(zip_code2),
    full_width_address = VALUES(full_width_address),
    half_width_address = VALUES(half_width_address);

INSERT INTO main_policy_address (policy_no, policy_seq, address_type, zip_code3, zip_code2, full_width_address, half_width_address)
VALUES
    ('P000000001', 1, '02', '104', '01', '臺北市中山區南京東路二段１００號', 'No.100, Sec.2, Nanjing E. Rd., Zhongshan Dist., Taipei City'),
    ('P000000001', 1, '31', NULL, NULL, 'policyholder@example.com', 'policyholder@example.com')
ON DUPLICATE KEY UPDATE
    zip_code3 = VALUES(zip_code3),
    zip_code2 = VALUES(zip_code2),
    full_width_address = VALUES(full_width_address),
    half_width_address = VALUES(half_width_address);

INSERT INTO main_policy_ride (policy_no, policy_seq, ride_type, ride_order, product_code, policy_years, insured_amount, premium)
VALUES
    ('P000000001', 1, '1', '000', 'LIFE', 20, 1000000.00, 12345.6789),
    ('P000000001', 1, '2', '001', 'ADDR', 20, 500000.00, 2345.6700),
    ('P000000001', 1, '3', '002', 'FAMI', 20, 300000.00, 1234.5600)
ON DUPLICATE KEY UPDATE
    ride_type = VALUES(ride_type),
    product_code = VALUES(product_code),
    policy_years = VALUES(policy_years),
    insured_amount = VALUES(insured_amount),
    premium = VALUES(premium);
