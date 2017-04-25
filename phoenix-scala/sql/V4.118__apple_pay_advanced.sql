alter table apple_payments alter column stripe_customer_id type text;
alter table credit_card_charges rename column charge_id to stripe_charge_id;
