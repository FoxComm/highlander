-- todo stripe token id starts with tok_
--create domain stripe_id_string text not null constraint valid_stripe_token check (length(value) > 0);

alter table return_cc_payments rename to return_stripe_payments;

alter table return_payments drop constraint valid_payment_type;
alter table return_payments add constraint valid_payment_type check
  (payment_method_type in ('creditCard', 'giftCard', 'storeCredit', 'applePay'));