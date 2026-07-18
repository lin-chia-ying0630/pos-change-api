ALTER TABLE policy_change_acceptance
    ADD COLUMN updated_by VARCHAR(128) NULL AFTER updated_at;
