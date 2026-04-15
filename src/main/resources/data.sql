-- seed a sample config entry and corresponding audit row
INSERT INTO CONFIG_ENTRY (config_key, config_value, rollout_percent, updated_by, updated_at, version)
VALUES ('feature.newCheckout', 'true', 10, 'seed', CURRENT_TIMESTAMP, 1);

INSERT INTO AUDIT_ENTRY (config_key, updated_by, rollout_percent, config_value, created_at, version)
VALUES ('feature.newCheckout', 'seed', 10, 'true', CURRENT_TIMESTAMP, 1);

-- additional sample configs
INSERT INTO CONFIG_ENTRY (config_key, config_value, rollout_percent, updated_by, updated_at, version)
VALUES ('payments.payments.prod.timeoutMs', '5000', 100, 'seed', CURRENT_TIMESTAMP, 1);

INSERT INTO AUDIT_ENTRY (config_key, updated_by, rollout_percent, config_value, created_at, version)
VALUES ('payments.payments.prod.timeoutMs', 'seed', 100, '5000', CURRENT_TIMESTAMP, 1);

INSERT INTO CONFIG_ENTRY (config_key, config_value, rollout_percent, updated_by, updated_at, version)
VALUES ('retail.website.staging.experiment.signupColor', 'blue', 25, 'seed', CURRENT_TIMESTAMP, 1);

INSERT INTO AUDIT_ENTRY (config_key, updated_by, rollout_percent, config_value, created_at, version)
VALUES ('retail.website.staging.experiment.signupColor', 'seed', 25, 'blue', CURRENT_TIMESTAMP, 1);

INSERT INTO CONFIG_ENTRY (config_key, config_value, rollout_percent, updated_by, updated_at, version)
VALUES ('loans.backend.feature.fastPath', 'false', 0, 'seed', CURRENT_TIMESTAMP, 1);

INSERT INTO AUDIT_ENTRY (config_key, updated_by, rollout_percent, config_value, created_at, version)
VALUES ('loans.backend.feature.fastPath', 'seed', 0, 'false', CURRENT_TIMESTAMP, 1);
