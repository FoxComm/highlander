
// libs
import React from 'react';
import types from '../base/types';

// components
import GiftCardCode from '../../../gift-cards/gift-card-code';
import GiftCard from './gift-card';
import OrderTarget from '../base/order-target';
import Currency from '../../../common/currency';

const representatives = {
  [types.GIFT_CARD_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>created gift card</strong> with code <GiftCardCode value={data.giftCard.code} />.
        </span>
      );
    },
    details: data => {
      return {
        newOne: <GiftCard giftCard={data.giftCard} />,
        previous: null,
      };
    },
  },
  // todo: do we have previous status ?
  [types.GIFT_CARD_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed state for gift card</strong> to {data.giftCard.status}.
        </span>
      );
    }
  },
  [types.GIFT_CARD_CONVERTED_TO_STORE_CREDIT]: {
    title: data => {
      return (
        <span>
          <strong>converted gift card</strong> <GiftCardCode value={data.giftCard.code} />
          &nbsp;to store credit.
        </span>
      );
    }
  },
  [types.GIFT_CARD_AUTHORIZED_FUNDS]: {
    title: data => {
      return (
        <span>
          <strong>authorized funds</strong> for <OrderTarget order={data.order} />
          &nbsp;with amount <Currency value={data.amount} />.
        </span>
      );
    },
  },
  [types.GIFT_CARD_CAPTURED_FUNDS]: {
    title: data => {
      return (
        <span>
          <strong>captured funds</strong> for <OrderTarget order={data.order} />
          &nbsp;with amount <Currency value={data.amount} />.
        </span>
      );
    },
  },
};

export default representatives;
