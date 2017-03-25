alter table amazon_categories add column category_name varchar(100);
update amazon_categories
  set category_name='clothing'
  where id in(select id from amazon_categories ac WHERE ac.node_path ilike 'Cloth%');