drop table return_lock_events;

alter table returns drop column is_locked;

drop table cart_lock_events;

alter table carts drop column is_locked;
