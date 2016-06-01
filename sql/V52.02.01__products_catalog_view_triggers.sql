create or replace function insert_products_catalog_search_view_from_products_fn() returns trigger as $$
begin

  insert into products_catalog_view select
    p.id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->(s.attributes->'images'->>'ref') as images,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    sku.price as sale_price,
    sku.currency as currency,
    f.attributes->>(s.attributes->'tags'->>'ref') as tags
    from products as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join object_forms as f on (f.id = p.form_id)
      inner join object_shadows as s on (s.id = p.shadow_id)
      inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
      inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
    where p.id = NEW.id and
          (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
          (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
          ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));


    return null;
end;
$$ language plpgsql;


create trigger insert_products_search_view_from_products
    after insert on products
    for each row
    execute procedure insert_products_catalog_search_view_from_products_fn();