
import React from 'react';
import types from '../base/types';

import OrderTarget from '../base/order-target';
import PaymentMethod from '../../../payment/payment-method';
import Currency from '../../../common/currency';
import GiftCardLink from '../base/gift-card-link';

const paymentMethodTitles = {
  giftCard: 'gift card',
  creditCard: 'credit card',
  storeCredit: 'store credit',
};


const representatives = {
  [types.ORDER_PAYMENT_METHOD_ADDED_CREDIT_CARD]: {
    title: data => {
      return (
        <span>
          <strong>added credit card</strong> ending in {data.creditCard.lastFour}
          &nbsp;as payment method for <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.ORDER_PAYMENT_METHOD_ADDED_GIFT_CARD]: {
    title: data => {
      return (
        <span>
          <strong>added gift card</strong> with code <GiftCardLink {...data.giftCard} />
          &nbsp;as payment method for <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.ORDER_PAYMENT_METHOD_DELETED_GIFT_CARD]: {
    title: data => {
      return (
        <span>
          <strong>removed gift card</strong> with code <GiftCardLink {...data.giftCard} />
          &nbsp;from payment method for <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.ORDER_PAYMENT_METHOD_ADDED_STORE_CREDIT]: {
    title: data => {
      return (
        <span>
          <strong>added store credit</strong> with amount <Currency value={data.amount} />
          &nbsp;as payment method for <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
  [types.ORDER_PAYMENT_METHOD_DELETED]: {
    title: data => {
      return (
        <span>
          <strong>removed payment method</strong> {paymentMethodTitles[data.pmt]}
          &nbsp;from <OrderTarget order={data.order} />.
        </span>
      );
    },
  },
};

export default representatives;
