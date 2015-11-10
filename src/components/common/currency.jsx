import React, { PropTypes } from 'react';

const isNumber = (n) => !isNaN(parseFloat(n)) && isFinite(n);

const formatCurrency = (amount, currency = '$') => {
  if (!isNumber(amount)) {
    return null;
  }
  const parsed = parseFloat(amount).toFixed(2);
  const decimal = parsed.slice(0, -3);
  const fract = parsed.slice(-2);
  const delimited = decimal.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${currency}${delimited}.${fract}`;
};

const Currency = (props) => {
  return <span className="fc-currency">{formatCurrency(props.value)}</span>;
};

Currency.propTypes = {
  value: PropTypes.number.isRequired
};

export default Currency;
