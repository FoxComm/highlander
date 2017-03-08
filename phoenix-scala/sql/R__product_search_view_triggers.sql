create or replace function update_products_search_view_from_links_fn() returns trigger as $$
begin

    update products_search_view set
        skus = subquery.skus,
        retail_price = subquery.retail_price
        from (select
                p.id,
                link.skus,
                pv_form.attributes->(pv_shadow.attributes->'retailPrice'->>'ref')->>'value' as retail_price
            from products as p
            inner join product_to_variant_links_view as link on (link.product_id = p.id)
            inner join product_to_variant_links as pv_link on (pv_link.left_id = p.id)
            inner join product_variants as pv on (pv.id = pv_link.right_id)
            inner join object_forms as pv_form on (pv_form.id = pv.form_id)
            inner join object_shadows as pv_shadow on (pv_shadow.id = pv.shadow_id)
            where link.product_id = new.product_id) as subquery
        where subquery.id = products_search_view.id;

    return null;

end;
$$ language plpgsql;

create or replace function insert_products_search_view_from_products_fn() returns trigger as $$
begin

  insert into products_search_view select
    new.id as id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    f.attributes->>(s.attributes->'tags'->>'ref') as tags,

    link.skus as skus,
    albumLink.albums as albums,
    to_json_timestamp(new.archived_at) as archived_at,
    f.attributes->>(s.attributes->'externalId'->>'ref') as external_id,
    p.scope as scope,
    p.slug as slug,
    '[]'::jsonb as taxonomies,
    to_json_timestamp(new.created_at) as created_at,
    (select
       pv_form.attributes->(pv_shadow.attributes->'retailPrice'->>'ref')->>'value'
       from product_variants as pv
         inner join product_to_variant_links as pv_link on (pv.id = pv_link.right_id)
           inner join object_forms as pv_form on (pv_form.id = pv.form_id)
             inner join object_shadows as pv_shadow on (pv_shadow.id = pv.shadow_id)
       where pv_link.left_id = p.id
       order by 1 -- which variant is first?
       limit 1
    ) as retail_price

    from products as p
    inner join object_contexts as context on (p.context_id = context.id)
    inner join object_forms as f on (f.id = p.form_id)
    inner join object_shadows as s on (s.id = p.shadow_id)
    left join product_to_variant_links_view as link on (link.product_id = p.id)
    left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
    where p.id = new.id;

    return null;
end;
$$ language plpgsql;
