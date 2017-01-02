update carriers set scope = '1.2' where scope is null;
update inventory_search_view set scope = '1.2' where scope is null;
update inventory_transactions_search_view set scope = '1.2' where scope is null;
update shipments set scope = '1.2' where scope is null;
update shipping_methods set scope = '1.2' where scope is null;
update stock_locations set scope = '1.2' where scope is null;

alter table carriers alter column scope set not null;
alter table inventory_search_view alter column scope set not null;
alter table inventory_transactions_search_view alter column scope set not null;
alter table shipments alter column scope set not null;
alter table shipping_methods alter column scope set not null;
alter table stock_locations alter column scope set not null;

