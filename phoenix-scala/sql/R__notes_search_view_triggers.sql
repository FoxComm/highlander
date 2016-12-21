-- variant
create or replace function update_notes_search_view_on_sku_fn() returns trigger as $$
begin

  update notes_search_view set variant_item = q.sku from
          (select
            to_json((
                f.id,
                v.code,
                to_json_timestamp(v.created_at)
            )::export_skus_raw)
              as sku
            from product_variants as v
            inner join object_forms as f on (f.id = v.form_id and f.kind = 'variant')
            where v.form_id = new.form_id) as q
        where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'variant';

  return null;
end;
$$ language plpgsql;