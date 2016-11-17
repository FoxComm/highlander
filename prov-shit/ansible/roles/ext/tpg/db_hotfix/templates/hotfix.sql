-- Reasons
INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('giftCardCreation', 1, 'Generic Gift Card Creation Reason');
INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('storeCreditCreation', 1, 'Generic Store Credit Creation Reason');
INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('cancellation', 1, 'Generic Cancellation Reason');

-- Scopes
DELETE FROM scopes WHERE id IN (2, 3);
DELETE FROM scope_domains WHERE id IN (2, 3);

INSERT INTO scopes ("source", "parent_id", "parent_path") VALUES ('org', 1, 1)
INSERT INTO scope_domains ("scope_id", "domain")
    SELECT max(id), 'theperfectgourmet.com' FROM scopes;

-- Remove useless admins
DELETE FROM admin_data WHERE id IN (2, 3);
DELETE FROM users WHERE id IN (2, 3);

DELETE FROM account_access_methods WHERE id IN (2, 3);
DELETE FROM account_organizations WHERE id IN (2, 3);
DELETE FROM account_roles WHERE id IN (2, 3);
DELETE FROM accounts WHERE id IN (2, 3);
DELETE FROM store_admins_search_view WHERE id IN (2, 3);
