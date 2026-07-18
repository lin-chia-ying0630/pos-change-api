ALTER TABLE main_policy_master
    ADD COLUMN created_by VARCHAR(128) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

ALTER TABLE main_policy_address
    ADD COLUMN created_by VARCHAR(128) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

ALTER TABLE main_policy_ride
    ADD COLUMN created_by VARCHAR(128) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

ALTER TABLE policy_change_item
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

ALTER TABLE policy_change_field
    ADD COLUMN created_by VARCHAR(128) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

ALTER TABLE policy_change_file
    ADD COLUMN created_by VARCHAR(128) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_by VARCHAR(128) NULL,
    ADD COLUMN reviewed_at DATETIME NULL;
