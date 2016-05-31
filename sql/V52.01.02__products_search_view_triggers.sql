-- object forms
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

-- object shadows

create or replace function update_products_search_view_from_shadows_fn() returns trigger as $$
begin

  update products_search_view set
    title = subquery.title,
    images = subquery.images,
    description = subquery.description,
    active_from = subquery.active_from,
    active_to = subquery.active_to,
    tags = subquery.tags
    from (select
            p.id,
            f.attributes->>(s.attributes->'title'->>'ref') as title,
            f.attributes->(s.attributes->'images'->>'ref') as images,
            f.attributes->>(s.attributes->'description'->>'ref') as description,
            f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
            f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags
        from products as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        where s.id = NEW.id) as subquery
    where subquery.id = products_search_view.id;


    return null;
end;
$$ language plpgsql;


create trigger update_products_search_view_from_shadows
    after insert or update on object_shadows
    for each row
    execute procedure update_products_search_view_from_shadows_fn();


--- object contexts

create or replace function update_products_search_view_from_context_fn() returns trigger as $$
begin

 update products_search_view set
    context = subquery.name
    from (select
            p.id,
            context.name
        from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        where context.id = NEW.id) as subquery
    where subquery.id = products_search_view.id;


    return null;

end;
$$ language plpgsql;

create trigger update_products_search_view_from_context_update
    after update on object_contexts
    for each row
    when (OLD.name is DISTINCT FROM NEW.name)
    execute procedure update_products_search_view_from_context_fn();

create trigger update_products_search_view_from_context_insert
    after insert on object_contexts
    for each row
    execute procedure update_products_search_view_from_context_fn();

-- product sku links
create or replace function update_products_search_view_from_links_fn() returns trigger as $$
begin

 update products_search_view set
    skus = subquery.skus
    from (select
            p.id,
            link.skus
        from products as p
        inner join product_sku_links_view as link on (link.product_id = p.id)
        where link.product_id = NEW.product_id) as subquery
    where subquery.id = products_search_view.id;


    return null;

end;
$$ language plpgsql;

create trigger update_products_search_view_from_links
    after insert or update on product_sku_links_view
    for each row
    execute procedure update_products_search_view_from_links_fn();