CREATE TABLE policy_change_case_sequence (
    sequence_date DATE NOT NULL,
    current_serial BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sequence_date)
);

ALTER TABLE policy_change_field
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE policy_change_file
    ADD COLUMN change_key VARCHAR(128) NULL AFTER change_file,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

UPDATE policy_change_file
SET change_key = JSON_UNQUOTE(JSON_EXTRACT(content_after, '$.addressType'))
WHERE change_key IS NULL
  AND change_file = 'main_policy_address'
  AND JSON_VALID(content_after);

DELETE older
FROM policy_change_field older
JOIN policy_change_field newer
  ON newer.policy_no = older.policy_no
 AND newer.policy_seq = older.policy_seq
 AND newer.change_case_no = older.change_case_no
 AND newer.change_item = older.change_item
 AND newer.change_field = older.change_field
 AND newer.change_key <=> older.change_key
 AND newer.id > older.id;

DELETE older
FROM policy_change_file older
JOIN policy_change_file newer
  ON newer.policy_no = older.policy_no
 AND newer.policy_seq = older.policy_seq
 AND newer.change_case_no = older.change_case_no
 AND newer.change_item = older.change_item
 AND newer.change_file = older.change_file
 AND newer.change_key <=> older.change_key
 AND newer.id > older.id;

CREATE UNIQUE INDEX uk_policy_change_acceptance_case_no
    ON policy_change_acceptance (change_case_no);

CREATE UNIQUE INDEX uk_policy_change_field_current
    ON policy_change_field (
        policy_no,
        policy_seq,
        change_case_no,
        change_item,
        change_field,
        change_key
    );

CREATE UNIQUE INDEX uk_policy_change_file_current
    ON policy_change_file (
        policy_no,
        policy_seq,
        change_case_no,
        change_item,
        change_file,
        change_key
    );
