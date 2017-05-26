create or replace function update_skus_view_from_object_attrs_fn() returns trigger as $$
begin
  update sku_search_view set
    sku_code = subquery.code,
    title = subquery.title,
    sale_price = subquery.sale_price,
    sale_price_currency = subquery.sale_price_currency,
    archived_at = subquery.archived_at,
    retail_price = subquery.retail_price,
    retail_price_currency = subquery.retail_price_currency,
    external_id = subquery.external_id
    from (select
        sku.id,
        sku.code,
        sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as sale_price,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as sale_price_currency,
        to_char(sku.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'value' as retail_price,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'currency' as retail_price_currency,
        sku_form.attributes->(sku_shadow.attributes->'externalId'->>'ref') as external_id
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = new.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function refresh_sku_search_view_fn() returns trigger as $$
declare
  sku_ids int[];

begin
  case tg_table_name
    when 'skus' then
      sku_ids := array_agg(new.id);

    when 'products' then
      select
        coalesce(
          array_agg(DISTINCT sku.id),
          array_agg(DISTINCT vvsl.right_id)
        ) into strict sku_ids
      from products as product
        left join product_sku_links as psl on (product.id = psl.left_id)
        left join skus as sku on (psl.right_id = sku.id)
        left join product_variant_links as pvl on (product.id = pvl.left_id)
        left join variant_variant_value_links as vvvl on (pvl.right_id = vvvl.left_id)
        left join variant_value_sku_links as vvsl on (vvvl.right_id = vvsl.left_id)
      where
        product.id = new.id
      group by
        product.id;

    when 'variant_values' then
      select
        array_agg(DISTINCT vvsl.right_id) into strict sku_ids
      from variant_values as variant_value
        inner join variant_value_sku_links as vvsl on (variant_value.id = vvsl.left_id)
      where variant_value.id = new.id;

    when 'images' then
      select
        coalesce(
          coalesce(sal.left_id, psl.right_id),
          vvsl.right_id
        ) into strict_sku_ids
      from images as image
        inner join album_image_links as ail on (image.id = ail.right_id)
        left join sku_album_links as sal on (ail.left_id = sal.right_id)
        left join product_album_links as pal on (image.id = pal.right_id)
        left join product_sku_links as psl on (pal.right_id = psl.left_id)
        left join product_variant_links as pvl on (psl.left_id = pvl.left_id)
        left join variant_variant_value_links as vvvl on (pvl.right_id = vvvl.left_id)
        left join variant_value_sku_links as vvsl on (vvvl.right_id = vvsl.left_id)
      where
        image.id = new.id;
  end case;

  if array_length(sku_ids, 1) > 0 then
    update sku_search_view set
      sku_code = subquery.sku_code,
      title = subquery.title,
      image = subquery.image,
      sale_price = subquery.sale_price,
      sale_price_currency = subquery.sale_price_currency,
      retail_price = subquery.retail_price,
      retail_price_currency = subquery.retail_price_currency,
      archived_at = subquery.archived_at

      from (
        select
	        sku.id as id,
 	        sku.code as sku_code,
 	        sku.context_id as context_id,
	        to_char(sku.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,

	        -- Use product title and fall back to SKU title if it doesn't exist
	        coalesce(
	  	      (array_agg(illuminate_text(product_form, product_shadow, 'title')))[1],
	  	      (array_agg(illuminate_text(sku_form, sku_shadow, 'title')))[1]
	        ) as title,
	        coalesce(
	  	      (array_agg(product_album_query.images))[1],
	  	      (array_agg(sku_album_query.images))[1]
	        ) as image,
	        illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'value' as sale_price,
	        illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'currency' as sale_price_currency,
	        illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'value' as retail_price,
	        illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'currency' as retail_price_currency,
	        sku_product_query.variants as subtitle

        from
	        skus as sku
	        inner join (
	  	      select
      		    sku.id as sku_id, 
      		    sku.context_id as context_id,
     		 	    coalesce(
                (array_agg(DISTINCT product.id))[1]::text,
                (array_agg(DISTINCT product_fv.id))[1]::text
              )::integer as product_id,
              array_agg(DISTINCT variant.id) as variant_ids,
      		    array_agg(DISTINCT variant_value.id) as value_ids,
      		    array_to_string(array_agg(illuminate_text(variant_value_form, variant_value_shadow, 'name'), ', ') as variants
	  	      from skus as sku
	  		      -- Find a product with no variants
	  		      left join product_sku_links as psl on (sku.id = psl.right_id)
	  		      left join products as product on (product.id = psl.left_id and product.context_id = sku.context_id)
	  
	  		      -- Find a product with variants
	  		      left join variant_value_sku_links as vvsl on (sku.id = vvsl.right_id)
	  		      left join variant_values as variant_value on (vvsl.left_id = variant_value.id and variant_value.context_id = sku.context_id)
	  
	  		      left join variant_variant_value_links as vvvl on (vvsl.left_id = vvvl.right_id)
	  		      left join variants as variant on (vvvl.left_id = variant.id and variant.context_id = sku.context_id)
	  
	  		      left join product_variant_links as pvl on (vvvl.left_id = pvl.right_id)
	  		      left join products as product_fv on (pvl.left_id = product_fv.id and product_fv.context_id = sku.context_id)
	  		
	  		      -- Compute a subtitle from variant values
	  		      left join object_forms as variant_value_form on (variant_value_form.id = variant_value.form_id)
	  		      left join object_shadows as variant_value_shadow on (variant_value_shadow.id = variant_value.shadow_id)
	  	      group by
	  		      sku.id, 
	  		      sku.context_id
	        ) as sku_product_query on (sku_product_query.sku_id = sku.id and sku_product_query.context_id = sku.context_id)
	  
	        inner join object_forms as sku_form on (sku_form.id = sku.form_id)
	        inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
	  
	        inner join products as product on (product.id = sku_product_query.product_id)
	        inner join object_forms as product_form on (product_form.id = product.form_id)
	        inner join object_shadows as product_shadow on (product_shadow.id = product.shadow_id)
	  
	        left join (
	  	      select 
	  		      product.id as id,
	  		      product.context_id as context_id,
	  		      (array_agg(illuminate_obj(image_form, image_shadow, 'src')))[1] as images	
	          from products as product
	  		      left join product_album_links as pal on (product.id = pal.left_id)
	  		      left join albums as album on (album.id = pal.right_id and album.context_id = product.context_id)
	  		      left join album_image_links as ail on (ail.left_id = album.id)
	  		      left join images as image on (image.id = ail.right_id)
	  		      left join object_forms as image_form on (image_form.id = image.form_id)
	  		      left join object_shadows as image_shadow on (image_shadow.id = image.shadow_id)
	        	group by
	  		      product.id
	        ) as product_album_query on (product_album_query.id = sku_product_query.product_id and product_album_query.context_id = sku.context_id)
	  
	        left join (
	  	      select 
	  		      sku.id as id,
	  		      sku.context_id as context_id,
	  		      (array_agg(illuminate_obj(image_form, image_shadow, 'src')))[1] as images
	  	      from skus as sku
	  		      left join sku_album_links as sal on (sku.id = sal.left_id)
	  		      left join albums as album on (album.id = sal.right_id and album.context_id = sku.context_id)
	  		      left join album_image_links as ail on (ail.left_id = album.id)
	  		      left join images as image on (image.id = ail.right_id)
	  		      left join object_forms as image_form on (image_form.id = image.form_id)
	  		      left join object_shadows as image_shadow on (image_shadow.id = image.shadow_id)
	  	      group by
	  		      sku.id
	        ) as sku_album_query on (sku_album_query.id = sku.id and sku_album_query.context_id = sku.context_id)
	  	
        where
          sku_product_query.product_id is not null
	  
        group by
	        sku.id,
	        sku.context_id,
	        sku_form.*,
	        sku_shadow.*,
	        sku_product_query.variants
      ) as subquery
      where subquery.id = any(sku_ids);
    end if;


  return null;
 end;
$$ language plpgsql;

