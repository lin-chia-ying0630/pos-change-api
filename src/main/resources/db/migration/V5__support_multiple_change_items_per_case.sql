CREATE TABLE policy_change_case_reservation_item (
    change_case_no VARCHAR(20) NOT NULL,
    change_item VARCHAR(3) NOT NULL,
    PRIMARY KEY (change_case_no, change_item),
    CONSTRAINT fk_change_case_reservation_item
        FOREIGN KEY (change_case_no)
        REFERENCES policy_change_case_reservation (change_case_no)
        ON DELETE CASCADE
);

INSERT INTO policy_change_case_reservation_item (change_case_no, change_item)
SELECT change_case_no, change_item
FROM policy_change_case_reservation;

ALTER TABLE policy_change_case_reservation
    DROP COLUMN change_item;
