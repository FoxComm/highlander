create materialized view products_search_view as
select
    p.id, 
    product_context.name as context,
    p.attributes->'title'->>(ps.attributes->>'title') as title,
    p.attributes->'images'->(ps.attributes->>'images') as images,
    p.attributes->'description'->(ps.attributes->>'description') as description
from products as p, product_shadows as ps, product_contexts as product_context 
where ps.product_id = p.id and ps.product_context_id = product_context.id;

create unique index products_search_view_idex on products_search_view (id, context);
