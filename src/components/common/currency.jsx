import React, { PropTypes } from 'react';
import formatCurrency from '../../lib/format-currency';

const Currency = (props) => {
  return <span className="fc-currency">{formatCurrency(props.value, props.base, props.currency)}</span>;
};

Currency.propTypes = {
  value: PropTypes.number.isRequired,
  base: PropTypes.number,
  currency: PropTypes.string
};

Currency.defaultProps = {
  base: 100,
  currency: '$'
};

export default Currency;
