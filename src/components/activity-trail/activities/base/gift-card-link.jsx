
import React, { PropTypes } from 'react';
import GiftCardCode from '../../../gift-cards/gift-card-code';
import { Link } from '../../../link';

const GiftCardLink = props => {
  return (
    <Link className="fc-activity__link" to="giftcard" params={{giftCard: props.code}}>
      <GiftCardCode value={props.code} />
    </Link>
  );
};

GiftCardLink.propTypes = {
  code: PropTypes.string,
};

export default GiftCardLink;
