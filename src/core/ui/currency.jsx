/* @flow */

import React, { PropTypes } from 'react';
import formatCurrency from 'lib/format-currency';
import type { HTMLElement } from 'types';

type CurrencyProps = {
  value: any,
  fractionBase: number,
  currency: string,
  bigNumber: bool,
  prefix: string,
  className?: string,
};

const Currency = (props: CurrencyProps): HTMLElement => {
  const { value, className, prefix, ...rest} = props;

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
