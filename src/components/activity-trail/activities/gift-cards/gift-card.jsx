
// libs
import React, { PropTypes } from 'react';
import static_url from '../../../../lib/s3';

// components
import GiftCardCode from '../../../gift-cards/gift-card-code';
import Currency from '../../../common/currency';

const GiftCard = props => {
  const icon = static_url('images/payments/payment_gift_card.png');
  const { giftCard } = props;

  return (
    <div className="fc-gc-info">
      <img className="fc-gc-info__icon" src={icon} />
      <div className="fc-gc-info__details">
        <div className="fc-gc-info__code">
          <GiftCardCode value={giftCard.code} />
        </div>
        <div className="fc-gc-info__amount">
          <Currency value={giftCard.availableBalance} currency={giftCard.currency} />
        </div>
      </div>
    </div>
  );
};

export default GiftCard;
