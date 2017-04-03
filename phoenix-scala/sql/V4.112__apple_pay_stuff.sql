
create table apple_pay_charges(
  id integer primary key,
  account_id integer null references accounts(id) on update restrict on delete restrict,
  gateway_customer_id text,
  currency currency,
  amount int,
  created_at generic_timestamp,
  updated_at generic_timestamp
);

-- todo
-- create domain for charge type
-- order_payment foreign key
-- returns?
