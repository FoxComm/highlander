INSERT INTO carriers ("name", "tracking_template")
VALUES ('UPS', 'https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=');

INSERT INTO shipping_methods ("carrier_id", "name", "code")
    SELECT max(id), 'UPS 2-Day Ground', 'UPS_GROUND' from carriers;

INSERT INTO shipping_methods ("carrier_id", "name", "code")
    SELECT max(id), 'UPS 2-Day Air', 'UPS_AIR' from carriers;
