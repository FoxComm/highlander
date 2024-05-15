-- set sequence value as max id + 1
create sequence if not exists  orders_search_view_id_seq increment by 1;

select setval ('orders_search_view_id_seq',
               coalesce((select max (id) + 1 from orders_search_view), 1), false);

alter table orders_search_view
   alter column id set default nextval ('orders_search_view_id_seq');
