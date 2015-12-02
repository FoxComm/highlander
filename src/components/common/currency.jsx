import React, { PropTypes } from 'react';
import formatCurrency from '../../lib/format-currency';

const Currency = (props) => {
  return <span className="fc-currency">{formatCurrency(props.value, {...props})}</span>;
};

Currency.propTypes = {
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  fractionBase: PropTypes.number,
  currency: PropTypes.string,
  bigNumber: PropTypes.bool
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false
};

export default Currency;
