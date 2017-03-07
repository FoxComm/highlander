alter table credit_card_charges add unique (charge_id);

create table return_cc_payments (
  id serial not null,
  return_payment_id integer not null references return_payments(id) on update cascade on delete restrict,
  charge_id generic_string not null references credit_card_charges(charge_id) on update cascade on delete restrict,
  return_id integer not null references returns(id) on update cascade on delete restrict,
  amount integer not null check (amount > 0),
  primary key (return_payment_id, charge_id)
);
