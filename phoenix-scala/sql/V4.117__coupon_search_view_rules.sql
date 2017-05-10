alter table coupons_search_view
    add column max_uses_per_code     integer null
  , add column max_uses_per_customer integer null
  ;

update coupons_search_view
  set
    max_uses_per_code     = q.max_uses_per_code
  , max_uses_per_customer = q.max_uses_per_customer
  from (
    select
      cp.form_id
    , (illuminate_obj(f, s, 'usageRules')->>'usesPerCode')     :: integer as max_uses_per_code
    , (illuminate_obj(f, s, 'usageRules')->>'usesPerCustomer') :: integer as max_uses_per_customer
    from       coupons         as cp
    inner join object_contexts as context on (cp.context_id = context.id)
    inner join object_forms    as f       on (f.id          = cp.form_id)
    inner join object_shadows  as s       on (s.id          = cp.shadow_id)
  ) as q
  where
    coupons_search_view.id = q.form_id
  ;
