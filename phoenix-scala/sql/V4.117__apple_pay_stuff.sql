
create table apple_payments(
  id integer primary key,
  account_id integer null references accounts(id) on update restrict on delete restrict,
  gateway_token_id text,
  gateway_customer_id text,
  deleted_at generic_timestamp,
  created_at generic_timestamp,
  foreign key (id) references payment_methods(id) on update restrict on delete restrict
);

create domain ap_payment_state text constraint valid_ap_payment_state check (
  value in ('cart','auth','failedAuth','canceledAuth','failedCapture', 'fullCapture'));

create table apple_pay_charges(
  id serial primary key,
  gateway_customer_id text,
  order_payment_id int null references order_payments(id) on update restrict on delete restrict,
  charge_id text,
  state ap_payment_state,
  currency currency,
  amount int,
  deleted_at generic_timestamp,
  created_at generic_timestamp
);

create trigger set_payment_method_id_trg
before insert
  on apple_payments
for each row
execute procedure set_payment_method_id();

alter table order_payments drop constraint valid_payment_type;
alter table order_payments add constraint valid_payment_type check
  (payment_method_type in ('creditCard', 'giftCard', 'storeCredit', 'applePay'));

