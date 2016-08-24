create table customer_password_resets(
  id serial primary key,
  customer_id integer not null references customers(id) on update restrict on delete restrict,
  email generic_string,
  state generic_string not null default 'initial',
  code generic_string not null unique,
  created_at generic_timestamp
);

create unique index customer_password_resets__m_idx on customer_password_resets (email,customer_id,state);

