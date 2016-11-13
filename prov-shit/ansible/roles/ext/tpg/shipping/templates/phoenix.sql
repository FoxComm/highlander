UPDATE shipping_methods SET is_active = false WHERE 1=1;
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'FEDEX_GROUND';
UPDATE shipping_methods SET code = left(md5(random()::text), 20) WHERE code = 'FEDEX_AIR';

WITH shippingMethodVars AS (
  SELECT
    '{"comparison":"and","conditions":[],"statements":[{"comparison":"and","conditions":[{"field":"regionName","valString":"Connecticut, Delaware, Florida, Illinois, Indiana, Maine, Maryland, Massachusetts, Michigan, North Carolina, New Hampshire, New Jersey, New York, Ohio, Pennsylvania, Rhode Island, Virginia, Vermont, West Virginia","operator":"inArray","rootObject":"ShippingAddress"}],"statements":[]}]}'::jsonb as fedexGround,
    '{"comparison":"and","conditions":[],"statements":[{"comparison":"and","conditions":[{"field":"regionName","valString":"Connecticut, Delaware, Florida, Illinois, Indiana, Maine, Maryland, Massachusetts, Michigan, North Carolina, New Hampshire, New Jersey, New York, Ohio, Pennsylvania, Rhode Island, Virginia, Vermont, West Virginia","operator":"notInArray","rootObject":"ShippingAddress"}],"statements":[]}]}'::jsonb as fedexAir
)
INSERT INTO shipping_methods
    ("admin_display_name", "storefront_display_name", "shipping_carrier_id", "price", "is_active", "conditions", "restrictions", "code")
VALUES
    ('FedEx 2-Day Ground', 'FedEx 2-Day Ground', NULL, 1495, TRUE, (SELECT fedExGround FROM shippingMethodVars), NULL, 'FEDEX_GROUND'),
    ('FedEx 2-Day Air', 'FedEx 2-Day Air', NULL, 3000, TRUE, (SELECT fedExAir FROM shippingMethodVars), NULL, 'FEDEX_AIR');
