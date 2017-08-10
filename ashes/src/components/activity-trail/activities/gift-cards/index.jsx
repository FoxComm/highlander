
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import GiftCardLink from '../base/gift-card-link';
import CordTarget from '../base/cord-target';
import Currency from 'components/utils/currency';
import Title from '../base/title';

const authorizedAndCapturedDesc = {
  title: (data, activity) => {
    const giftCards = data.giftCardCodes.map(code => <GiftCardLink key={code} code={code} />);
    const giftCardText = giftCards.length == 1 ? 'gift card' : 'gift cards';

    const actionTitle = activity.kind == types.GIFT_CARD_AUTHORIZED_FUNDS ?
      'authorized funds' : 'captured funds';
    const order = data.order || data.cart;

    return (
      <Title activity={activity}>
        <strong>{actionTitle}</strong> for <CordTarget cord={order} />
        &nbsp;with amount <Currency value={data.amount} /> from {joinEntities(giftCards)} {giftCardText}
      </Title>
    );
  },
};

const representatives = {
  [types.GIFT_CARD_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created gift card</strong> <GiftCardLink {...data.giftCard} />
          &nbsp; with amount <Currency value={data.giftCard.availableBalance} currency={data.giftCard.currency} />
        </Title>
      );
    },
  },
  // todo: do we have previous state ?
  [types.GIFT_CARD_STATE_CHANGED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed the state for gift card</strong> <GiftCardLink {...data.giftCard} />
          &nbsp;to {data.giftCard.state}
        </Title>
      );
    }
  },
  [types.GIFT_CARD_CONVERTED_TO_STORE_CREDIT]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>converted gift card</strong> <GiftCardLink {...data.giftCard} />
          &nbsp;to store credit with amount
          &nbsp;<Currency value={data.storeCredit.availableBalance} currency={data.storeCredit.currency} />
        </Title>
      );
    }
  },
  [types.GIFT_CARD_AUTHORIZED_FUNDS]: authorizedAndCapturedDesc,
  [types.GIFT_CARD_CAPTURED_FUNDS]: authorizedAndCapturedDesc,
};

export default representatives;
