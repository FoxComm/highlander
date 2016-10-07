create or replace function update_store_credits_view_from_store_admins_fn() returns trigger as $$
declare store_credit_ids integer[];
begin
  case tg_table_name
    when 'store_credits' then
      store_credit_ids := array_agg(new.id);
    when 'store_credits_search_view' then
      store_credit_ids := array_agg(new.id);
    when 'store_credit_manuals' then
      select array_agg(sc.id) into strict store_credit_ids
      from store_credit_manuals as scm
      inner join store_credits as sc on (sc.origin_id = scm.id)
      where scm.id = new.id and sc.origin_type = 'csrAppeasement';
    when 'store_credit_customs' then
      select array_agg(sc.id) into strict store_credit_ids
      from store_credit_customs as scs
      inner join store_credits as sc on (sc.origin_id = scs.id)
      where scs.id = new.id and sc.origin_type = 'custom';
    when 'store_admins' then
      select array_agg(sc.id) into strict store_credit_ids
      from store_credits as sc
      left join store_credit_manuals as scm on (sc.origin_id = scm.id)
      left join store_credit_customs as scc on (sc.origin_id = scc.id)
      left join store_admins as sa on (sa.id = scm.admin_id or sa.id = scc.admin_id)
      where sa.id = new.id;
  end case;

  update store_credits_search_view set
    store_admin = subquery.store_admin
    from (select
            sc.id,
            case when count(sa) = 0 then '{}'
            else to_json((sa.email, sa.name, sa.department)::export_store_admins)::jsonb
            end as store_admin
        from store_credits as sc
        left join store_credit_manuals as scm on (sc.origin_id = scm.id)
        left join store_credit_customs as scc on (sc.origin_id = scc.id)
        left join store_admins as sa on (sa.id = scm.admin_id or sa.id = scc.admin_id)
        where sc.id = any(store_credit_ids)
        group by sc.id, sa.id) as subquery
    where store_credits_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

drop trigger update_store_credits_view_from_store_admins_store_credits_fn on store_credits;
create trigger update_store_credits_view_from_store_admins_store_credits_fn
    after update on store_credits
    for each row
    execute procedure update_store_credits_view_from_store_admins_fn();

create trigger update_store_credits_view_admins_from_self_tg
  after insert on store_credits_search_view
  for each row
  execute procedure update_store_credits_view_from_store_admins_fn();

drop trigger update_store_credits_view_from_store_admins_store_credit_manuals_fn on store_credit_manuals;
create trigger update_store_credits_view_from_store_admins_store_credit_manuals_fn
    after update on store_credit_manuals
    for each row
    execute procedure update_store_credits_view_from_store_admins_fn();

drop trigger update_store_credits_view_from_store_admins_store_credit_customs_fn on store_credit_customs;
create trigger update_store_credits_view_from_store_admins_store_credit_customs_fn
    after update on store_credit_customs
    for each row
    execute procedure update_store_credits_view_from_store_admins_fn();


drop trigger update_store_credits_view_from_store_admins_store_admins_fn on store_admins;
create trigger update_store_credits_view_from_store_admins_store_admins_fn
    after update on store_admins
    for each row
    execute procedure update_store_credits_view_from_store_admins_fn();
