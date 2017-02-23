----- update views
alter table product_variants_search_view
      add column created_at json_timestamp,
      add column product_id integer;

alter table products_search_view
      add column created_at json_timestamp,
      add column retail_price text;

update product_variants_search_view set
  created_at = q.created_at,
  product_id = q.product_id
  from (select
          pv.form_id,
          p.form_id as product_id,
          to_json_timestamp(pv.created_at) as created_at
          from product_variants as pv
            inner join product_to_variant_links as pv_link on (pv.id = pv_link.right_id)
              inner join products as p on (p.id = pv_link.left_id)
       ) as q
  where id = q.form_id;

update products_search_view set
  created_at = q.created_at,
  retail_price = q.retail_price
  from (select
          p.form_id,
          to_json_timestamp(p.created_at) as created_at,
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
       ) as q
  where id = q.form_id;

alter table product_variants_search_view
      alter column created_at set not null,
      alter column product_id set not null;

alter table products_search_view
      alter column created_at set not null,
      alter column retail_price set not null;
