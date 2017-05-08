import React from 'react';
import PropTypes from 'prop-types';
import { formatCode } from '../../lib/gift-card-utils';

const GiftCardCode = props => {
  return <span className="fc-gift-card-code">{formatCode(props.value)}</span>;
};

GiftCardCode.propTypes = {
  value: PropTypes.string.isRequired
};

export default GiftCardCode;
