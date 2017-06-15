
import React from 'react';
import types from '../base/types';

import CordTarget from '../base/cord-target';
import Currency from 'components/utils/currency';
import GiftCardLink from '../base/gift-card-link';
import Title from '../base/title';

const paymentMethodTitles = {
  giftCard: 'gift card',
  creditCard: 'credit card',
  storeCredit: 'store credit',
};


const representatives = {
  [types.CART_PAYMENT_METHOD_ADDED_CREDIT_CARD]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>added credit card</strong> ending in {data.creditCard.lastFour}
          &nbsp;as payment method for <CordTarget cord={cord} />
        </Title>
      );
    },
  },
  [types.CART_PAYMENT_METHOD_ADDED_GIFT_CARD]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>added gift card</strong> with code <GiftCardLink {...data.giftCard} />
          &nbsp;as payment method for <CordTarget cord={cord} />
        </Title>
      );
    },
  },
  [types.CART_PAYMENT_METHOD_DELETED_GIFT_CARD]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>removed gift card</strong> with code <GiftCardLink {...data.giftCard} />
          &nbsp;from payment method for <CordTarget cord={cord} />
        </Title>
      );
    },
  },
  [types.CART_PAYMENT_METHOD_ADDED_STORE_CREDIT]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>added store credit</strong> with amount <Currency value={data.amount} />
          &nbsp;as payment method for <CordTarget cord={cord} />
        </Title>
      );
    },
  },
  [types.CART_PAYMENT_METHOD_DELETED]: {
    title: (data, activity) => {
      const cord = data.order || data.cart;
      return (
        <Title activity={activity}>
          <strong>removed payment method</strong> {paymentMethodTitles[data.pmt]}
          &nbsp;from <CordTarget cord={cord} />
        </Title>
      );
    },
  },
};

export default representatives;
