create or replace function update_products_search_view_from_forms_fn() returns trigger as $$
begin

  update products_search_view set
    product_id = subquery.product_id,
    title = subquery.title,
    images = subquery.images,
    description = subquery.description,
    active_from = subquery.active_from,
    active_to = subquery.active_to,
    tags = subquery.tags
    from (select
            p.id,
            f.id as product_id,
            f.attributes->>(s.attributes->'title'->>'ref') as title,
            f.attributes->(s.attributes->'images'->>'ref') as images,
            f.attributes->>(s.attributes->'description'->>'ref') as description,
            f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
            f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags
        from products as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        where f.id = NEW.id) as subquery
    where subquery.id = products_search_view.id;


    return null;
end;
$$ language plpgsql;


create trigger update_products_search_view_from_forms
    after insert or update on object_forms
    for each row
    when (NEW.kind = 'product')
    execute procedure update_products_search_view_from_forms_fn();