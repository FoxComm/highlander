----- update views
alter table product_variants_search_view
      add column created_at json_timestamp,
      add column product_id integer,
      add column album_id integer;

alter table products_search_view
      add column created_at json_timestamp,
      add column retail_price text; -- why string? :(

update product_variants_search_view set
  created_at = q.created_at,
  product_id = q.product_id,
  album_id = q.album_id
  from (select
          pv.form_id,
          p.form_id as product_id,
          to_json_timestamp(pv.created_at) as created_at,
          pa_link.right_id as album_id
          from product_variants as pv
            left join product_to_variant_links as pv_link on (pv.id = pv_link.right_id)
              left join products as p on (p.id = pv_link.left_id)
                left join product_album_links as pa_link on (p.id = pa_link.left_id)
       ) as q
  where id = q.form_id;

update products_search_view set
  created_at = q.created_at,
  retail_price = q.retail_price
  from (select
          p.form_id,
          to_json_timestamp(p.created_at) as created_at,
          p_form.attributes->>(p_shadow.attributes->'retailPrice'->>'ref') as retail_price
          from products as p
            left join object_forms as p_form on (p_form.id = p.form_id)
              left join object_shadows as p_shadow on (p_shadow.id = p.shadow_id)
       ) as q
  where id = q.form_id;

alter table product_variants_search_view
      alter column created_at set not null
      -- , alter column product_id set not null
      -- , alter column album_id set not null
      ;

alter table products_search_view
      alter column created_at set not null
      -- , alter column retail_price set not null
      ;
