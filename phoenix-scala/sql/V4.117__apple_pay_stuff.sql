create domain stripe_id_string text not null constraint valid_stripe_token check (length(value) > 0);

create table apple_payments(
  id integer primary key,
  account_id integer null references accounts(id) on update restrict on delete restrict,
  stripe_token_id stripe_id_string,
  stripe_customer_id stripe_id_string,
  deleted_at generic_timestamp,
  created_at generic_timestamp not null,
  foreign key (id) references payment_methods(id) on update restrict on delete restrict
);

create domain applepay_payment_state text constraint valid_applepay_payment_state check (
  value in ('cart','auth','failedAuth','canceledAuth','failedCapture', 'fullCapture'));

create table apple_pay_charges(
  id serial primary key,
  order_payment_id int null references order_payments(id) on update restrict on delete restrict,
  stripe_charge_id stripe_id_string,
  state applepay_payment_state,
  currency currency,
  amount int,
  deleted_at generic_timestamp,
  created_at generic_timestamp not null
);

create trigger set_payment_method_id_trg
before insert
  on apple_payments
for each row
execute procedure set_payment_method_id();

alter table order_payments drop constraint valid_payment_type;
alter table order_payments add constraint valid_payment_type check
  (payment_method_type in ('creditCard', 'giftCard', 'storeCredit', 'applePay'));

