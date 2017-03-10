alter domain assignment_ref_type drop constraint assignment_ref_type_check;

alter domain assignment_ref_type add check (value in ('cart', 'order', 'giftCard',
    'customer', 'return', 'product', 'sku', 'promotion', 'coupon', 'taxonomy'));
