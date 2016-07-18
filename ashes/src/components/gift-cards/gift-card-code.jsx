import React, { PropTypes } from 'react';
import { formatCode } from '../../lib/gift-card-utils';

const GiftCardCode = props => {
  return <span className="fc-gift-card-code">{formatCode(props.value)}</span>;
};

GiftCardCode.propTypes = {
  value: PropTypes.string.isRequired
};

export default GiftCardCode;
