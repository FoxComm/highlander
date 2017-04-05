
create domain ap_payment_state text constraint valid_ap_payment_state check (value in ('CART', 'STATUS_SUCCESS', 'STATUS_FAILURE'));

create table apple_pay_charges(
  id integer primary key,
  account_id integer null references accounts(id) on update restrict on delete restrict,
  gateway_customer_id text,
  order_payment_id int null references order_payments(id) on update restrict on delete restrict,
  state text, -- todo domain here
  currency currency,
  amount int,
  deleted_at generic_timestamp,
  created_at generic_timestamp,
  foreign key (id) references payment_methods(id) on update restrict on delete restrict
);

-- very questionable. this connects apple_pay_charges to payment_method_id
create trigger set_payment_method_id_trg
before insert
  on apple_pay_charges
for each row
execute procedure set_payment_method_id();


alter table order_payments drop constraint valid_payment_type;
alter table order_payments add constraint valid_payment_type check
  (payment_method_type in ('creditCard', 'giftCard', 'storeCredit', 'applePay'));

-- todo domain? we have deps on it, so need to recreate other stuff
-- create domain order_payment_type text constraint valid_payment_type check (value in
--                                                                            ('creditCard', 'giftCard', 'storeCredit', 'applePay'));
-- alter table order_payments alter column payment_method_type type order_payment_type;


-- todo
-- create domain for charge type
-- order_payment foreign key
-- returns?
