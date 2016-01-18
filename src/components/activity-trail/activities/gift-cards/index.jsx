
// libs
import React from 'react';
import types from '../base/types';
import { joinEntities } from '../base/utils';

// components
import GiftCardCode from '../../../gift-cards/gift-card-code';
import OrderTarget from '../base/order-target';
import Currency from '../../../common/currency';

const authorizedAndCapturedDesc = {
  title: (data, {kind}) => {
    const giftCards = data.giftCardCodes.map(code => <GiftCardCode value={code} />);
    const giftCardText = giftCards.length == 1 ? 'gift card' : 'gift cards';

    const actionTitle = kind == types.GIFT_CARD_AUTHORIZED_FUNDS ?
      'authorized funds' : 'captured funds';

    return (
      <span>
        <strong>{actionTitle}</strong> for <OrderTarget order={data.order} />
        &nbsp;with amount <Currency value={data.amount} /> from {joinEntities(giftCards)} {giftCardText}.
      </span>
    );
  },
};

const representatives = {
  [types.GIFT_CARD_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>created gift card</strong> <GiftCardCode value={data.giftCard.code} />
          &nbsp; with amount <Currency value={data.giftCard.availableBalance} currency={data.giftCard.currency} />.
        </span>
      );
    },
  },
  // todo: do we have previous status ?
  [types.GIFT_CARD_STATE_CHANGED]: {
    title: data => {
      return (
        <span>
          <strong>changed state for gift card</strong> <GiftCardCode value={data.giftCard.code} />
          &nbsp;to {data.giftCard.status}.
        </span>
      );
    }
  },
  [types.GIFT_CARD_CONVERTED_TO_STORE_CREDIT]: {
    title: data => {
      return (
        <span>
          <strong>converted gift card</strong> <GiftCardCode value={data.giftCard.code} />
          &nbsp;to store credit with amount
          &nbsp;<Currency value={data.storeCredit.availableBalance} currency={data.storeCredit.currency} />.
        </span>
      );
    }
  },
  [types.GIFT_CARD_AUTHORIZED_FUNDS]: authorizedAndCapturedDesc,
  [types.GIFT_CARD_CAPTURED_FUNDS]: authorizedAndCapturedDesc,
};

export default representatives;
