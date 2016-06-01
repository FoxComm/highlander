-- designed for updating products_catalog_vew by scheduled job
-- when activity period affected

create or replace function toggle_products_catalog_from_to_active() returns boolean as $$
begin

-- delete outdated products (active -> inactive transition by time)
delete from products_catalog_view where id IN (select p.id
  from products as p
  inner join products_catalog_view as pv on (pv.id = p.id)
  inner join object_forms as f on (f.id = p.form_id)
  inner join object_shadows as s on (s.id = p.shadow_id)
  where
    ((f.attributes->>(s.attributes->'activeFrom'->>'ref')) = '') IS NOT FALSE
    or
    (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp >= CURRENT_TIMESTAMP
    or
      (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS FALSE and
      ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp < CURRENT_TIMESTAMP)));




return true;

end;
$$ LANGUAGE plpgsql;