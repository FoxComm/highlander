INSERT INTO carriers ("name", "tracking_template")
VALUES ('FedEx', 'http://www.fedex.com/Tracking?action=track&tracknumbers=');

INSERT INTO shipping_methods ("carrier_id", "name", "code")
SELECT max(id), 'FedEx 2-Day Ground', 'FEDEX_GROUND' from carriers;

INSERT INTO shipping_methods ("carrier_id", "name", "code")
SELECT max(id), 'FedEx 2-Day Air', 'FEDEX_AIR' from carriers;
