-- object forms
create or replace function update_products_search_view_from_attrs_fn() returns trigger as $$
begin

  update products_search_view set
    product_id = subquery.product_id,
    title = subquery.title,
    description = subquery.description,
    active_from = subquery.active_from,
    active_to = subquery.active_to,
    tags = subquery.tags,
    archived_at = subquery.archived_at
    from (select
            p.id,
            f.id as product_id,
            f.attributes->>(s.attributes->'title'->>'ref') as title,
            f.attributes->>(s.attributes->'description'->>'ref') as description,
            f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
            f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags,
            to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
        from products as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        where p.id = NEW.id) as subquery
    where subquery.id = products_search_view.id;


    return null;
end;
$$ language plpgsql;


create trigger update_products_search_view_from_attrs
    after update on products
    for each row
    execute procedure update_products_search_view_from_attrs_fn();

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

-- product album links
create or replace function update_products_search_view_from_album_links_fn() returns trigger as $$
begin

  update products_search_view set
    albums = subquery.albums
    from (select
            p.id,
            link.albums
        from products as p
        inner join product_album_links_view as link on (link.product_id = p.id)
        where link.product_id = NEW.product_id) as subquery
    where subquery.id = products_search_view.id;

    return null;

end;
$$ language plpgsql;

create trigger update_products_search_view_from_album_links
    after insert or update on product_album_links_view
    for each row
    execute procedure update_products_search_view_from_album_links_fn();
