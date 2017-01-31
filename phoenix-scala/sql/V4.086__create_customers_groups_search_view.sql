alter table customer_dynamic_groups add column scope exts.ltree;
update customer_dynamic_groups set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
alter table customer_dynamic_groups alter column scope set not null;

create table customer_groups_search_view (
  id bigint primary key,
  group_id bigint not null,
  name generic_string,
  customers_count integer null,
  updated_at json_timestamp,
  created_at json_timestamp,
  scope exts.ltree
);

-- Shared Searches
alter domain shared_search_scope drop constraint shared_search_scope_check;

alter domain shared_search_scope add constraint shared_search_scope_check check (value in (
                                                        'customersScope', 'customerGroupsScope', 'ordersScope',
                                                        'storeAdminsScope', 'giftCardsScope', 'productsScope',
                                                        'inventoryScope', 'promotionsScope', 'couponsScope',
                                                        'couponCodesScope', 'skusScope', 'cartsScope'));