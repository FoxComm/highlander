alter table products_search_view add column form jsonb;
alter table products_search_view add column shadow jsonb;

update products_search_view
  set
    form = q.form,
    shadow = q.shadow
        from (select
                p.id,
                f.attributes as form,
                s.attributes as shadow
                  from products as p
                    inner join object_forms as f on (f.id = p.form_id)
                    inner join object_shadows as s on (s.id = p.shadow_id)
        ) as q
  where products_search_view.id = q.id;


alter table products_search_view alter column form set not null;
alter table products_search_view alter column shadow set not null;
