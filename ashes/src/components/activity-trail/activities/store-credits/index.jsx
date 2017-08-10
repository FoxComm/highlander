
// libs
import React from 'react';
import types from '../base/types';

// components
import GiftCardLink from '../base/gift-card-link';
import CordTarget from '../base/cord-target';
import Currency from 'components/utils/currency';
import Title from '../base/title';

const representatives = {
  [types.STORE_CREDIT_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created store credit</strong> with amount
          &nbsp;<Currency value={data.storeCredit.availableBalance} currency={data.storeCredit.currency} />
        </Title>
      );
    },
  },
  // todo: do we have previous state and customer ?
  [types.STORE_CREDIT_STATE_CHANGED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed the state for store credit</strong> to {data.storeCredit.state}
        </Title>
      );
    }
  },
  [types.STORE_CREDIT_CONVERTED_TO_GIFT_CARD]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>converted store credit</strong> to gift card <GiftCardLink {...data.giftCard} />
          &nbsp;with amount <Currency value={data.giftCard.availableBalance} currency={data.giftCard.currency} />.
        </Title>
      );
    },
  },
  [types.STORE_CREDIT_AUTHORIZED_FUNDS]: {
    title: data => {
      const cord = data.order || data.cart;
      return (
        <span>
          <strong>authorized funds</strong> for <CordTarget cord={cord} />
          &nbsp;with amount <Currency value={data.amount} /> from store credit.
        </span>
      );
    },
  },
  [types.STORE_CREDIT_CAPTURED_FUNDS]: {
    title: data => {
      const cord = data.order || data.cart;

      return (
        <span>
          <strong>captured funds</strong> for <CordTarget cord={cord} />
          &nbsp;with amount <Currency value={data.amount} /> from store credit.
        </span>
      );
    },
  },
};

export default representatives;
