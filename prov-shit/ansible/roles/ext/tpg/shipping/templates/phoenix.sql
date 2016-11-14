UPDATE shipping_methods SET is_active = false WHERE 1=1;
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'FEDEX_GROUND';
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'FEDEX_AIR';
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'UPS_GROUND';
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'UPS_AIR';

WITH shippingMethodVars AS (
  SELECT
    '{"comparison":"and","conditions":[],"statements":[{"comparison":"and","conditions":[{"field":"regionName","valString":"Connecticut, Delaware, Florida, Illinois, Indiana, Maine, Maryland, Massachusetts, Michigan, North Carolina, New Hampshire, New Jersey, New York, Ohio, Pennsylvania, Rhode Island, Virginia, Vermont, West Virginia","operator":"inArray","rootObject":"ShippingAddress"}],"statements":[]}]}'::jsonb as upsGround,
    '{"comparison":"and","conditions":[],"statements":[{"comparison":"and","conditions":[{"field":"regionName","valString":"Connecticut, Delaware, Florida, Illinois, Indiana, Maine, Maryland, Massachusetts, Michigan, North Carolina, New Hampshire, New Jersey, New York, Ohio, Pennsylvania, Rhode Island, Virginia, Vermont, West Virginia","operator":"notInArray","rootObject":"ShippingAddress"}],"statements":[]}]}'::jsonb as upsAir
)
INSERT INTO shipping_methods
    ("admin_display_name", "storefront_display_name", "shipping_carrier_id", "price", "is_active", "conditions", "restrictions", "code")
VALUES
    ('UPS 2-Day Ground', 'UPS 2-Day Ground', NULL, 1495, TRUE, (SELECT upsGround FROM shippingMethodVars), NULL, 'UPS_GROUND'),
    ('UPS 2-Day Air', 'UPS 2-Day Air', NULL, 3000, TRUE, (SELECT upsAir FROM shippingMethodVars), NULL, 'UPS_AIR');
