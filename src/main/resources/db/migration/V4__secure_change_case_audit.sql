CREATE TABLE policy_change_case_reservation (
    change_case_no VARCHAR(20) NOT NULL,
    policy_no VARCHAR(10) NOT NULL,
    policy_seq INT NOT NULL,
    change_item VARCHAR(3) NOT NULL,
    reserved_by VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (change_case_no),
    INDEX idx_change_case_reservation_expiry (expires_at, consumed_at),
    CONSTRAINT fk_change_case_reservation_master
        FOREIGN KEY (policy_no, policy_seq)
        REFERENCES main_policy_master (policy_no, policy_seq)
);

ALTER TABLE policy_change_acceptance
    ADD COLUMN created_by VARCHAR(128) NOT NULL DEFAULT 'legacy-migration' AFTER acceptance_status,
    ADD COLUMN reviewed_by VARCHAR(128) NULL AFTER created_by,
    ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by;

CREATE INDEX idx_policy_change_acceptance_creator
    ON policy_change_acceptance (created_by, policy_no, created_at);
