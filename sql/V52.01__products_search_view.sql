create table products_search_view
(
    id integer,
    product_id integer,
    context generic_string,
    title text,
    images jsonb,
    description text,
    active_from text,
    active_to text,
    tags text,
    skus jsonb
);
create unique index products_search_view_idx on products_search_view (id, lower(context));

create or replace function insert_products_search_view_from_products_fn() returns trigger as $$
begin

  insert into products_search_view select
    NEW.id as id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->(s.attributes->'images'->>'ref') as images,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    f.attributes->>(s.attributes->'tags'->>'ref') as tags,
    link.skus as skus
    from products as p
    inner join object_contexts as context on (p.context_id = context.id)
    inner join object_forms as f on (f.id = p.form_id)
    inner join object_shadows as s on (s.id = p.shadow_id)
    left join product_sku_links_view as link on (link.product_id = p.id)
    where p.id = NEW.id;


    return null;
end;
$$ language plpgsql;


create trigger insert_products_search_view_from_products
    after insert on products
    for each row
    execute procedure insert_products_search_view_from_products_fn();

