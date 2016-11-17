INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('giftCardCreation', 1, 'Generic Gift Card Creation Reason');
INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('storeCreditCreation', 1, 'Generic Store Credit Creation Reason');
INSERT INTO reasons ("reason_type", "store_admin_id", "body") VALUES ('cancellation', 1, 'Generic Cancellation Reason');

DELETE FROM scopes WHERE id = 2 AND id = 3;
DELETE FROM scope_domains WHERE id = 2 AND id = 3;

INSERT INTO scopes ("source", "parent_id", "parent_path") VALUES ('org', 1, 1)
INSERT INTO scope_domains ("scope_id", "domain")
    SELECT max(id), 'theperfectgourmet.com' FROM scopes;