
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';
import GiftCardCode from '../../../gift-cards/gift-card-code';

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
