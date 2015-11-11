import React, { PropTypes } from 'react';

const isNumber = (n) => !isNaN(parseFloat(n)) && isFinite(n);

const formatCurrency = (amount, base, currency) => {
  if (!isNumber(amount)) {
    return null;
  }
  const parsed = parseFloat(amount / base).toFixed(2);
  const decimal = parsed.slice(0, -3);
  const fract = parsed.slice(-2);
  const delimited = decimal.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${currency}${delimited}.${fract}`;
};

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
