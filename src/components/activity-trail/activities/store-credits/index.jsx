
// libs
import React from 'react';
import types from '../base/types';

// components
import GiftCardLink from '../base/gift-card-link';;
import OrderTarget from '../base/order-target';
import CustomerLink from '../base/customer-link';
import Currency from '../../../common/currency';

const representatives = {
  [types.STORE_CREDIT_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>created store credit</strong> with amount
          &nbsp;<Currency value={data.storeCredit.availableBalance} currency={data.storeCredit.currency} />
          &nbsp; for customer <CustomerLink customer={data.customer} />.
        </span>
      );
    },
  },
  // todo: do we have previous status and customer ?
  [types.STORE_CREDIT_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed the state for store credit</strong> to {data.storeCredit.state}.
        </span>
      );
    }
  },
  [types.STORE_CREDIT_CONVERTED_TO_GIFT_CARD]: {
    title: data => {
      return (
        <span>
          <strong>converted store credit</strong> to gift card <GiftCardLink {...data.giftCard} />
          &nbsp;with amount <Currency value={data.giftCard.availableBalance} currency={data.giftCard.currency} />.
        </span>
      );
    },
  },
  [types.STORE_CREDIT_AUTHORIZED_FUNDS]: {
    title: data => {
      return (
        <span>
          <strong>authorized funds</strong> for <OrderTarget order={data.order} />
          &nbsp;with amount <Currency value={data.amount} /> from store credit.
        </span>
      );
    },
  },
  [types.STORE_CREDIT_CAPTURED_FUNDS]: {
    title: data => {
      return (
        <span>
          <strong>captured funds</strong> for <OrderTarget order={data.order} />
          &nbsp;with amount <Currency value={data.amount} /> from store credit.
        </span>
      );
    },
  },
};

export default representatives;
