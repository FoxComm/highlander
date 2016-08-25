create domain reset_password_state text constraint valid_reset_pw_state check (value in
    ('initial', 'emailSend', 'disabled', 'passwordRestored'));


create table customer_password_resets(
  id serial primary key,
  customer_id integer not null references customers(id) on update restrict on delete restrict,
  email generic_string,
  state reset_password_state not null default 'initial',
  code generic_string not null unique,
  created_at generic_timestamp,
  activated_at timestamp without time zone
);

create unique index customer_password_resets__m_idx
  on customer_password_resets (email,customer_id,state)
  where state = 'initial';

create index customer_password_resets__customer_idx on customer_password_resets (email,customer_id);
