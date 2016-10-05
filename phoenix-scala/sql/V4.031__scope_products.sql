--adding scope to objects in the object store.

alter table albums add column scope exts.ltree not null;
alter table categories add column scope exts.ltree not null;
alter table coupons add column scope exts.ltree not null;
alter table discounts add column scope exts.ltree not null;
alter table images add column scope exts.ltree not null;
alter table products add column scope exts.ltree not null;
alter table promotions add column scope exts.ltree not null;
alter table skus add column scope exts.ltree not null;
alter table variant_values add column scope exts.ltree not null;
alter table variants add column scope exts.ltree not null;

update albums set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update categories set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update coupons set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update discounts set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update images set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update products set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update promotions set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update skus set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update variant_values set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update variants set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
