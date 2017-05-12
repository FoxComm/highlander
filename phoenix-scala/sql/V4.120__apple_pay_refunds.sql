alter table return_cc_payments rename to return_stripe_payments;
alter table return_stripe_payments alter column charge_id type stripe_id_string;

alter table return_payments drop constraint valid_payment_type;
alter table return_payments add constraint valid_payment_type check
  (payment_method_type in ('creditCard', 'giftCard', 'storeCredit', 'applePay'));
