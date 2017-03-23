/* @flow */

import React, { PropTypes, Element } from 'react';
import formatCurrency from 'lib/format-currency';

type CurrencyProps = {
  value: any,
  fractionBase?: number,
  currency?: string,
  bigNumber?: boolean,
  prefix?: string,
  className?: string,
};

const Currency = (props: CurrencyProps): Element<*> => {
  const { value, className, prefix = '', ...rest} = props;

  return (
    <span className={className}>
      {`${prefix}${formatCurrency(value, {...rest})}`}
    </span>
  );
};

Currency.propTypes = {
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  fractionBase: PropTypes.number,
  currency: PropTypes.string,
  bigNumber: PropTypes.bool,
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false,
  prefix: '',
};

export default Currency;
